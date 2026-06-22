package gamalsolutions.whatscustomreply.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import gamalsolutions.whatscustomreply.data.datastore.AppSettings
import gamalsolutions.whatscustomreply.data.datastore.SettingsManager
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import kotlinx.coroutines.CoroutineScope
import gamalsolutions.whatscustomreply.service.InteractiveVoiceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PhoneCallReceiver : BroadcastReceiver(), KoinComponent {
    private val settingsManager: SettingsManager by inject()
    private val logsRepository: LogsRepository by inject()
    private val customApiRepository: CustomApiRepository by inject()
    private val repliesRepository: RepliesRepository by inject()
    private val speechHelper: SpeechHelper by inject()
    private val interactiveVoiceHelper: InteractiveVoiceHelper by inject()

    companion object {
        private const val TAG = "PhoneCallReceiver"
        private var originalRingerMode: Int? = null
        private var originalRingerVolume: Int? = null
        private var lastIncomingNumber: String? = null
        private var isWasRinging = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            Log.d(TAG, "Phone State changed: $state, Number: $incomingNumber")

            val finalNumber = if (incomingNumber.isNotBlank()) incomingNumber else (lastIncomingNumber ?: "")

            CoroutineScope(Dispatchers.IO).launch {
                val settings = settingsManager.settingsFlow.firstOrNull() ?: return@launch
                if (!settings.isServiceEnabled) return@launch

                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (incomingNumber.isNotBlank()) {
                        lastIncomingNumber = incomingNumber
                    }
                    isWasRinging = true

                    if (originalRingerMode == null) {
                        originalRingerMode = audioManager.ringerMode
                    }
                    if (originalRingerVolume == null) {
                        originalRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    }

                    // 1. Dynamic Audio & Volume Handling
                    try {
                        val modeValue = when (settings.ringerMode) {
                            0 -> AudioManager.RINGER_MODE_SILENT
                            1 -> AudioManager.RINGER_MODE_VIBRATE
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }
                        audioManager.ringerMode = modeValue

                        if (modeValue == AudioManager.RINGER_MODE_NORMAL) {
                            val maxRingerVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                            val targetRingerVol = (settings.ringerVolume / 100.0 * maxRingerVolume).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, targetRingerVol, 0)
                        }

                        val maxMediaVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetMediaVol = (settings.mediaVolume / 100.0 * maxMediaVol).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetMediaVol, 0)

                        Log.d(TAG, "Audio configured successfully. Mode: $modeValue, Ringer: ${settings.ringerVolume}%, Media: ${settings.mediaVolume}%")
                    } catch (e: Exception) {
                        Log.e(TAG, "Could not set call audio volume: ${e.message}")
                    }

                    // For incoming calls ringing, trigger automated active reply
                    triggerCallReplyFlow(context, intent, finalNumber, settings, isMissed = false)

                } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    isWasRinging = false
                    if (settings.interactiveVoiceCallEnabled && finalNumber.isNotBlank()) {
                        Log.d(TAG, "EXTRA_STATE_OFFHOOK: Starting Interactive Voice Assistant Dialogue Flow.")
                        interactiveVoiceHelper.startDialogue(settings, finalNumber)
                    }
                } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                    val missedTriggered = isWasRinging && finalNumber.isNotBlank()
                    isWasRinging = false
                    interactiveVoiceHelper.shutdown()

                    try {
                        originalRingerMode?.let { mode ->
                            audioManager.ringerMode = mode
                            Log.d(TAG, "Restored original ringer mode: $mode")
                        }
                        originalRingerVolume?.let { rVol ->
                            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                audioManager.setStreamVolume(AudioManager.STREAM_RING, rVol, 0)
                                Log.d(TAG, "Restored original ringer volume: $rVol")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to restore original call audio configuration: ${e.message}")
                    } finally {
                        originalRingerMode = null
                        originalRingerVolume = null
                    }

                    if (missedTriggered) {
                        Log.d(TAG, "Detected Missed Call from: $finalNumber. Triggering missed call auto reply.")
                        triggerCallReplyFlow(context, intent, finalNumber, settings, isMissed = true)
                    }
                    lastIncomingNumber = null
                }
            }
        }
    }

    private suspend fun triggerCallReplyFlow(context: Context, intent: Intent, incomingNumber: String, settings: AppSettings, isMissed: Boolean) {
        if (!settings.callReplyEnabled || incomingNumber.isBlank()) return

        // Multi-Account verification
        if (!isPhoneAccountAllowed(context, intent, settings)) {
            Log.d(TAG, "Call reply skipped because incoming account verification failed.")
            return
        }

        try {
            var finalReplyText = settings.callReplyText
            var usedMode = if (isMissed) "CALL_RESPONDER_MISSED (STATIC)" else "CALL_RESPONDER (STATIC)"
            var matchedRuleType = "TEXT"

            val enabledReplies = repliesRepository.getEnabledReplies()
            var customMatchText: String? = null
            var matchedRule: CustomReplyEntity? = null

            for (reply in enabledReplies) {
                if (!isRuleTargetPhoneAllowed(reply, context, intent, settings)) {
                    continue
                }

                val normalizedIncoming = incomingNumber.trim().replace(" ", "").replace("+", "")
                val normalizedRuleContact = reply.contactName?.trim()?.replace(" ", "")?.replace("+", "") ?: ""

                val contactMatch = normalizedRuleContact.isNotEmpty() && normalizedIncoming.contains(normalizedRuleContact)
                val keywordMatch = reply.keyword.trim().equals("call", ignoreCase = true) || 
                    reply.keyword.trim().equals("phone", ignoreCase = true) ||
                    reply.keyword.trim().equals("اتصال", ignoreCase = true) ||
                    reply.keyword.trim().equals("مكالمة", ignoreCase = true) ||
                    reply.keyword.trim().equals("هاتف", ignoreCase = true)

                val isCallActiveTrigger = reply.triggerType == "CALL_ACTIVE"
                val isCallMissedTrigger = reply.triggerType == "CALL_MISSED"
                val isCallTrigger = isCallActiveTrigger || isCallMissedTrigger

                if (isCallTrigger) {
                    if (isMissed && isCallMissedTrigger) {
                        if (contactMatch || (keywordMatch && reply.contactName.isNullOrBlank())) {
                            customMatchText = reply.replyText
                            matchedRule = reply
                            usedMode = "CALL_RESPONDER_MISSED (CUSTOM_RULE)"
                            break
                        }
                    } else if (!isMissed && isCallActiveTrigger) {
                        if (contactMatch || (keywordMatch && reply.contactName.isNullOrBlank())) {
                            customMatchText = reply.replyText
                            matchedRule = reply
                            usedMode = "CALL_RESPONDER_ACTIVE (CUSTOM_RULE)"
                            break
                        }
                    }
                } else {
                    if (contactMatch) {
                        customMatchText = reply.replyText
                        matchedRule = reply
                        usedMode = "CALL_RESPONDER (CUSTOM_CONTACT_RULE)"
                        break
                    } else if (keywordMatch && reply.contactName.isNullOrBlank()) {
                        customMatchText = reply.replyText
                        matchedRule = reply
                        usedMode = "CALL_RESPONDER (CUSTOM_CALL_RULE)"
                    }
                }
            }

            if (customMatchText != null) {
                finalReplyText = customMatchText
                matchedRuleType = matchedRule?.replyType ?: "TEXT"
            } else if (settings.replyMode == "API" || settings.replyMode == "HYBRID") {
                val apiResult = customApiRepository.generateReply(
                    apiUrl = settings.apiUrl,
                    apiMethod = settings.apiMethod,
                    apiHeaders = settings.apiHeaders,
                    apiBodyTemplate = settings.apiBodyTemplate,
                    apiResponsePath = settings.apiResponsePath,
                    sender = incomingNumber,
                    message = if (isMissed) "Missed Call (Phone Call)" else "Incoming Call (Phone Call)"
                )
                apiResult.onSuccess { text ->
                    finalReplyText = text
                    usedMode = if (isMissed) "CALL_RESPONDER_MISSED (API)" else "CALL_RESPONDER (API)"
                }.onFailure { e ->
                    Log.e(TAG, "API call response extraction failed, falling back to static", e)
                    finalReplyText = "${settings.callReplyText} (API Error: ${e.localizedMessage ?: e.message})"
                    usedMode = if (isMissed) "CALL_RESPONDER_MISSED (API_FALLBACK)" else "CALL_RESPONDER (API_FALLBACK)"
                }
            }

            // If voice reply, format indicator
            if (matchedRuleType == "VOICE") {
                finalReplyText = if (settings.appLanguage == "en") {
                    "🎙️ [Voice Reply]: $finalReplyText"
                } else {
                    "🎙️ [رد صوتي]: $finalReplyText"
                }
            }

            // Send SMS back to incoming caller
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            smsManager.sendTextMessage(incomingNumber, null, finalReplyText, null, null)

            if (settings.voiceReplyEnabled || matchedRuleType == "VOICE") {
                val announceB = if (settings.appLanguage == "en") {
                    if (isMissed) "Missed call from $incomingNumber automatically replied: $finalReplyText" else "Incoming call from $incomingNumber automatically replied: $finalReplyText"
                } else {
                    if (isMissed) "مكالمة فائتة من $incomingNumber، تم الرد عليها تلقائياً بـ: $finalReplyText" else "مكالمة هاتفية واردة من $incomingNumber، تم الرد عليها تلقائياً بـ: $finalReplyText"
                }
                speechHelper.speak(announceB, settings.appLanguage)
            }

            logsRepository.insertLog(
                AutoReplyLogEntity(
                    senderName = incomingNumber,
                    messageText = if (isMissed) "Missed call" else "Incoming call",
                    replyText = finalReplyText,
                    mode = usedMode,
                    isSuccess = true
                )
            )
            Log.d(TAG, "Call auto-reply SMS sent successfully to: $incomingNumber. Type: $matchedRuleType")
        } catch (e: Exception) {
            Log.e(TAG, "Call auto-reply SMS send failed", e)
            try {
                logsRepository.insertLog(
                    AutoReplyLogEntity(
                        senderName = incomingNumber,
                        messageText = if (isMissed) "Missed call" else "Incoming call",
                        replyText = "${settings.callReplyText} (Failed: ${e.localizedMessage ?: e.message})",
                        mode = if (isMissed) "CALL_RESPONDER_MISSED_FAILED" else "CALL_RESPONDER_FAILED",
                        isSuccess = false
                    )
                )
            } catch (logEx: Exception) {
                Log.e(TAG, "Log database insertion failed", logEx)
            }
        }
    }

    private fun isPhoneAccountAllowed(context: Context, intent: Intent, settings: AppSettings): Boolean {
        if (settings.primaryAccountPhone.isBlank() && settings.additionalAccountPhones.isBlank()) {
            return true
        }

        val allowedAccounts = mutableListOf<String>()
        if (settings.primaryAccountPhone.isNotBlank()) {
            allowedAccounts.add(settings.primaryAccountPhone.trim())
        }
        if (settings.additionalAccountPhones.isNotBlank()) {
            settings.additionalAccountPhones.split(",").forEach {
                if (it.isNotBlank()) {
                    allowedAccounts.add(it.trim())
                }
            }
        }

        if (allowedAccounts.isEmpty()) return true

        // For cellular standard calls, we will try to resolve cellular SIM phone numbers from SubscriptionManager
        val discoveredSimNumbers = mutableListOf<String>()
        
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    for (info in activeList) {
                        val num = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try { subscriptionManager.getPhoneNumber(info.subscriptionId) } catch (e: Exception) { info.number }
                        } else {
                            @Suppress("DEPRECATION")
                            info.number
                        }
                        if (!num.isNullOrBlank()) {
                            discoveredSimNumbers.add(num)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.d(TAG, "No phone state permission to read SIM subscription numbers: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subscription info number: ${e.message}")
        }

        // Also add SIM subId or slot id as simple string representation in case users configure them (e.g. "slot_0", "slot_1")
        val slotId = intent.getIntExtra("slot", -1)
        val simId = intent.getIntExtra("simId", -1)
        val subId = intent.getIntExtra("subscription", -1)
        
        if (slotId != -1) discoveredSimNumbers.add("slot_$slotId")
        if (simId != -1) discoveredSimNumbers.add("slot_$simId")
        if (subId != -1) discoveredSimNumbers.add("sub_$subId")

        val normalizedAllowed = allowedAccounts.map { it.replace(" ", "").replace("+", "").replace("-", "") }
        val normalizedDiscovered = discoveredSimNumbers.map { it.replace(" ", "").replace("+", "").replace("-", "") }

        for (acc in normalizedAllowed) {
            for (sim in normalizedDiscovered) {
                if (sim.contains(acc, ignoreCase = true)) {
                    Log.d(TAG, "Confirmed: receiving cellular call matches allowed SIM account: $acc")
                    return true
                }
            }
        }

        // Safe Fallback: if Android / SubscriberManager doesn't supply SIM phone number (which is very common for many SIM cards),
        // we should not block cellular calls as long as there is no contradictory dual SIM info!
        // So we permit if we couldn't read any actual numeric SIM numbers from Telephony.
        val hasAnyDetectedSimNumber = normalizedDiscovered.any { text ->
            text.any { it.isDigit() } && text.filter { it.isDigit() }.length >= 5
        }

        if (!hasAnyDetectedSimNumber) {
            Log.d(TAG, "No valid cellular SIM phone number detected from Telephony. Safe fallback: permitting cellular call.")
            return true
        }

        Log.d(TAG, "Blocked cellular call because configured accounts did not match active SIM card slot/number.")
        return false
    }

    private fun isRuleTargetPhoneAllowed(reply: CustomReplyEntity, context: Context, intent: Intent, settings: AppSettings): Boolean {
        if (!isPhoneAccountAllowed(context, intent, settings)) return false

        val target = reply.targetAccount
        if (target.isNullOrBlank()) return true

        val normTarget = target.trim().replace(" ", "").replace("+", "").replace("-", "")

        val discoveredSimNumbers = mutableListOf<String>()
        try {
            val subscriptionManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subscriptionManager != null) {
                val activeList = subscriptionManager.activeSubscriptionInfoList
                if (activeList != null) {
                    for (info in activeList) {
                        val num = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            try { subscriptionManager.getPhoneNumber(info.subscriptionId) } catch (e: Exception) { info.number }
                        } else {
                            @Suppress("DEPRECATION")
                            info.number
                        }
                        if (!num.isNullOrBlank()) discoveredSimNumbers.add(num)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subscription info number on rule check: ${e.message}")
        }

        val slotId = intent.getIntExtra("slot", -1)
        val simId = intent.getIntExtra("simId", -1)
        val subId = intent.getIntExtra("subscription", -1)
        
        if (slotId != -1) discoveredSimNumbers.add("slot_$slotId")
        if (simId != -1) discoveredSimNumbers.add("slot_$simId")
        if (subId != -1) discoveredSimNumbers.add("sub_$subId")

        val normalizedDiscovered = discoveredSimNumbers.map { it.replace(" ", "").replace("+", "").replace("-", "") }

        for (sim in normalizedDiscovered) {
            if (sim.contains(normTarget, ignoreCase = true)) {
                return true
            }
        }

        return false
    }
}
