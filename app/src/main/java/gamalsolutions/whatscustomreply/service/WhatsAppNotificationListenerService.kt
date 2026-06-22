package gamalsolutions.whatscustomreply.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import gamalsolutions.whatscustomreply.data.datastore.AppSettings
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WhatsAppNotificationListenerService : NotificationListenerService(), KoinComponent {

    private val repliesRepository: RepliesRepository by inject()
    private val logsRepository: LogsRepository by inject()
    private val customApiRepository: CustomApiRepository by inject()
    private val settingsManager: gamalsolutions.whatscustomreply.data.datastore.SettingsManager by inject()
    private val speechHelper: SpeechHelper by inject()

    private val job = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    // In-memory cache for anti-duplicate & single reply checks
    private val processedMessageHashes = mutableSetOf<String>()
    private val repliedUsers = mutableSetOf<String>()

    companion object {
        private const val TAG = "AutoReplyService"
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val pkg = sbn.packageName
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            return
        }

        serviceScope.launch {
            val settings = settingsManager.settingsFlow.firstOrNull() ?: return@launch
            if (!settings.isServiceEnabled) {
                Log.d(TAG, "Service is globally disabled in settings.")
                return@launch
            }

            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val rawText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            if (title.isBlank() || rawText.isBlank()) {
                return@launch
            }

            // Standard WhatsApp notifications contain active reply markers, but let's filter out
            // notifications from "You" or messages indicating that we are transferring or uploading
            if (title == "You" || title.contains("Sending") || title.contains("Uploading")) {
                return@launch
            }

            // Account validation checks
            if (!isAccountAllowed(sbn, settings)) {
                Log.d(TAG, "Notification skipped because it does not match configured target account filters.")
                return@launch
            }

            // WhatsApp Call Handling
            val isCallNotification = rawText.contains("Missed voice call") || 
                    rawText.contains("Missed video call") || 
                    rawText.contains("Incoming call") ||
                    rawText.contains("مكالمة فائتة") ||
                    rawText.contains("مكالمة واردة") ||
                    rawText.contains("Incoming voice call") ||
                    rawText.contains("Incoming video call")

            if (isCallNotification) {
                Log.d(TAG, "WhatsApp Call Notification Detected from: $title. Status: $rawText")
                
                // Mute or configure audio profile if active ringing
                if (rawText.contains("Incoming") || rawText.contains("وارد")) {
                    try {
                        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val rMode = when (settings.ringerMode) {
                            0 -> AudioManager.RINGER_MODE_SILENT
                            1 -> AudioManager.RINGER_MODE_VIBRATE
                            else -> AudioManager.RINGER_MODE_NORMAL
                        }
                        audioManager.ringerMode = rMode
                        if (rMode == AudioManager.RINGER_MODE_NORMAL) {
                            val maxR = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                            val targetR = (settings.ringerVolume / 100.0 * maxR).toInt()
                            audioManager.setStreamVolume(AudioManager.STREAM_RING, targetR, 0)
                        }
                        val maxM = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetM = (settings.mediaVolume / 100.0 * maxM).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetM, 0)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed adjusting system volumes: ${e.message}")
                    }
                }

                // If Call Auto Reply is enabled, reply with callReplyText or custom API call
                if (settings.callReplyEnabled) {
                    var finalReplyText = settings.callReplyText
                    var usedMode = "WHATSAPP_CALL (STATIC)"
                    var matchedRuleType = "TEXT"

                    try {
                        // Match local custom replies rules for WhatsApp calls
                        val enabledReplies = repliesRepository.getEnabledReplies()
                        var customMatchText: String? = null
                        var matchedRule: CustomReplyEntity? = null
                        val isMissed = rawText.contains("Missed", ignoreCase = true) || rawText.contains("فائتة", ignoreCase = true)

                        for (reply in enabledReplies) {
                            if (!isRuleTargetAccountAllowed(sbn, reply, title, rawText, settings)) {
                                continue
                            }

                            val isCallActiveTrigger = reply.triggerType == "CALL_ACTIVE"
                            val isCallMissedTrigger = reply.triggerType == "CALL_MISSED"
                            val isCallTrigger = isCallActiveTrigger || isCallMissedTrigger

                            val contactMatch = !reply.contactName.isNullOrBlank() && 
                                reply.contactName.trim().equals(title.trim(), ignoreCase = true)
                            val keywordMatch = reply.keyword.trim().equals("call", ignoreCase = true) || 
                                reply.keyword.trim().equals("phone", ignoreCase = true) ||
                                reply.keyword.trim().equals("اتصال", ignoreCase = true) ||
                                reply.keyword.trim().equals("مكالمة", ignoreCase = true) ||
                                reply.keyword.trim().equals("هاتف", ignoreCase = true)

                            if (isCallTrigger) {
                                if (isMissed && isCallMissedTrigger) {
                                    if (contactMatch || (keywordMatch && reply.contactName.isNullOrBlank())) {
                                        customMatchText = reply.replyText
                                        matchedRule = reply
                                        usedMode = "WHATSAPP_CALL (CUSTOM_CALL_MISSED_RULE)"
                                        break
                                    }
                                } else if (!isMissed && isCallActiveTrigger) {
                                    if (contactMatch || (keywordMatch && reply.contactName.isNullOrBlank())) {
                                        customMatchText = reply.replyText
                                        matchedRule = reply
                                        usedMode = "WHATSAPP_CALL (CUSTOM_CALL_ACTIVE_RULE)"
                                        break
                                    }
                                }
                            } else {
                                // Default rules fallback
                                if (contactMatch) {
                                    customMatchText = reply.replyText
                                    matchedRule = reply
                                    usedMode = "WHATSAPP_CALL (CUSTOM_CONTACT_RULE)"
                                    break
                                } else if (keywordMatch && reply.contactName.isNullOrBlank()) {
                                    customMatchText = reply.replyText
                                    matchedRule = reply
                                    usedMode = "WHATSAPP_CALL (CUSTOM_CALL_RULE)"
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
                                sender = title,
                                message = "Incoming Call (WhatsApp): $rawText"
                            )
                            apiResult.onSuccess { text ->
                                finalReplyText = text
                                usedMode = "WHATSAPP_CALL (API)"
                            }.onFailure { e ->
                                Log.e(TAG, "WhatsApp Call API response failed, falling back to static", e)
                                finalReplyText = "${settings.callReplyText} (API Error: ${e.localizedMessage ?: e.message})"
                                usedMode = "WHATSAPP_CALL (API_FALLBACK)"
                            }
                        }

                        if (matchedRuleType == "VOICE") {
                            finalReplyText = if (settings.appLanguage == "en") {
                                "🎙️ [Voice Reply]: $finalReplyText"
                            } else {
                                "🎙️ [رد صوتي]: $finalReplyText"
                            }
                        }

                        val sendMethodSuccess = replyToNotification(sbn, finalReplyText)
                        
                        if (sendMethodSuccess && (settings.voiceReplyEnabled || matchedRuleType == "VOICE")) {
                            val announceText = if (settings.appLanguage == "en") {
                                "Automated reply sent to WhatsApp caller $title: $finalReplyText"
                            } else {
                                "تم الرد تلقائياً على مكالمة واتساب من $title: $finalReplyText"
                            }
                            speechHelper.speak(announceText, settings.appLanguage)
                        }

                        logsRepository.insertLog(
                            AutoReplyLogEntity(
                                senderName = title,
                                messageText = "WhatsApp Call: $rawText",
                                replyText = finalReplyText,
                                mode = usedMode,
                                isSuccess = sendMethodSuccess
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "WhatsApp Call Auto Reply crashed during execution but handled safely", e)
                        try {
                            logsRepository.insertLog(
                                AutoReplyLogEntity(
                                    senderName = title,
                                    messageText = "WhatsApp Call: $rawText",
                                    replyText = "Error handling auto reply: ${e.message}",
                                    mode = "WHATSAPP_CALL (SERVICE_ERROR)",
                                    isSuccess = false
                                )
                            )
                        } catch (nestedEx: Exception) {
                            Log.e(TAG, "Nested log write failure: ${nestedEx.message}")
                        }
                    }
                }

                if (settings.dismissNotificationsEnabled) {
                    try {
                        cancelNotification(sbn.key)
                        Log.d(TAG, "WhatsApp Call notification dismissed cleanly from screen.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed dismissing WhatsApp call: ${e.message}")
                    }
                }
                return@launch
            }

            // 1. Group Filtering
            val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false)
            if (isGroup && settings.ignoreGroups) {
                Log.d(TAG, "Skipping group message from: $title")
                return@launch
            }

            val messageUniqueHash = "$title|$rawText"

            // 2. Duplicate Filtering
            if (settings.ignoreDuplicates && processedMessageHashes.contains(messageUniqueHash)) {
                Log.d(TAG, "Skipping duplicate notification: $messageUniqueHash")
                return@launch
            }

            // 3. Simple single-response filter
            if (settings.replyOncePerUser && repliedUsers.contains(title)) {
                Log.d(TAG, "Skipping. Already replied to user: $title")
                return@launch
            }

            // 4. Working hours filter
            if (settings.workingHoursEnabled && !isWithinWorkingHours(settings.workingHoursStart, settings.workingHoursEnd)) {
                Log.d(TAG, "Skipping. Outside current working hours config.")
                return@launch
            }

            // Cache the message hash immediately to prevent processing double events
            processedMessageHashes.add(messageUniqueHash)

            Log.d(TAG, "Processing WhatsApp incoming message -> Sender: $title, Message: $rawText")

            // Execute processing logic
            processAndReply(sbn, title, rawText, settings)
        }
    }

    private suspend fun processAndReply(
        sbn: StatusBarNotification,
        sender: String,
        message: String,
        settings: AppSettings
    ) {
        val mode = settings.replyMode // "CUSTOM", "API", "HYBRID"
        var replyText: String? = null
        var logMode = "CUSTOM"
        var matchedReplyType = "TEXT"

        // Search Custom Keywords
        val matchedCustom = findCustomReplyMatch(sbn, message, sender, settings)

        if (mode == "CUSTOM") {
            if (matchedCustom != null) {
                replyText = matchedCustom.replyText
                matchedReplyType = matchedCustom.replyType
            }
            logMode = "CUSTOM"
        } else if (mode == "API") {
            logMode = "CUSTOM_API"
            val apiResult = customApiRepository.generateReply(
                apiUrl = settings.apiUrl,
                apiMethod = settings.apiMethod,
                apiHeaders = settings.apiHeaders,
                apiBodyTemplate = settings.apiBodyTemplate,
                apiResponsePath = settings.apiResponsePath,
                sender = sender,
                message = message
            )
            apiResult.onSuccess { text -> replyText = text }
            apiResult.onFailure { e -> Log.e(TAG, "Custom API reply generation failed: ${e.message}") }
        } else if (mode == "HYBRID") {
            if (matchedCustom != null) {
                replyText = matchedCustom.replyText
                matchedReplyType = matchedCustom.replyType
                logMode = "CUSTOM (HYBRID)"
            } else {
                logMode = "CUSTOM_API (HYBRID)"
                val apiResult = customApiRepository.generateReply(
                    apiUrl = settings.apiUrl,
                    apiMethod = settings.apiMethod,
                    apiHeaders = settings.apiHeaders,
                    apiBodyTemplate = settings.apiBodyTemplate,
                    apiResponsePath = settings.apiResponsePath,
                    sender = sender,
                    message = message
                )
                apiResult.onSuccess { text -> replyText = text }
                apiResult.onFailure { e -> Log.e(TAG, "Custom API hybrid reply failed: ${e.message}") }
            }
        }

        if (replyText != null && matchedReplyType == "VOICE") {
            replyText = if (settings.appLanguage == "en") {
                "🎙️ [Voice Reply]: $replyText"
            } else {
                "🎙️ [رد صوتي]: $replyText"
            }
        }

        // Random delay simulation if active
        if (replyText != null && settings.randomDelayEnabled) {
            val delayRange = settings.randomDelayMin..settings.randomDelayMax
            val delaySeconds = if (!delayRange.isEmpty()) delayRange.random() else settings.randomDelayMin
            Log.d(TAG, "Applying random reply delay: $delaySeconds seconds")
            delay(delaySeconds * 1000L)
        }

        // Send reply over RemoteInput
        val isSuccess = if (replyText != null) {
            replyToNotification(sbn, replyText!!)
        } else {
            false
        }

        // Log transaction to DB
        logsRepository.insertLog(
            AutoReplyLogEntity(
                senderName = sender,
                messageText = message,
                replyText = replyText ?: "No matching reply (Mode: $mode)",
                mode = logMode,
                isSuccess = isSuccess
            )
        )

        if (isSuccess && replyText != null) {
            repliedUsers.add(sender)
            Log.d(TAG, "Automated reply sent successfully using RemoteInput!")
            if (settings.voiceReplyEnabled || matchedReplyType == "VOICE") {
                val announceText = if (settings.appLanguage == "en") {
                    "Automated reply sent to $sender: $replyText"
                } else {
                    "تم الرد تلقائياً على $sender: $replyText"
                }
                speechHelper.speak(announceText, settings.appLanguage)
            }
        }

        if (settings.dismissNotificationsEnabled) {
            try {
                cancelNotification(sbn.key)
                Log.d(TAG, "WhatsApp chat notification with key ${sbn.key} dismissed cleanly from screen.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed dismissing WhatsApp chat: ${e.message}")
            }
        }
    }

    private suspend fun findCustomReplyMatch(sbn: StatusBarNotification, messageText: String, sender: String, settings: AppSettings): CustomReplyEntity? {
        val enabledReplies = repliesRepository.getEnabledReplies()
        for (reply in enabledReplies) {
            if (reply.triggerType != "CHAT") continue
            if (!isRuleTargetAccountAllowed(sbn, reply, sender, messageText, settings)) continue

            val contactMatch = reply.contactName.isNullOrBlank() || reply.contactName.trim().equals(sender.trim(), ignoreCase = true)
            if (contactMatch && messageText.contains(reply.keyword, ignoreCase = true)) {
                return reply
            }
        }
        return null
    }

    private fun isAccountAllowed(sbn: StatusBarNotification, settings: AppSettings): Boolean {
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

        // Extract metadata representing the receiving channel / account from the notification
        val textToMatch = mutableListOf<String>()

        val extras = sbn.notification.extras
        if (extras != null) {
            // Check subText (highly likely to hold account descriptor in Multi-Account / Dual WhatsApp)
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.let { textToMatch.add(it) }
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.let { textToMatch.add(it) }
            
            // Loop through all extras to extract any non-content, recipient-related fields
            try {
                for (key in extras.keySet()) {
                    if (key != Notification.EXTRA_TITLE && 
                        key != Notification.EXTRA_TEXT && 
                        key != "android.title" && 
                        key != "android.text" &&
                        key != "android.bigText") {
                        val value = extras.get(key)
                        if (value is CharSequence || value is String) {
                            textToMatch.add(value.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading extras on isAccountAllowed check: ${e.message}")
            }
        }

        sbn.tag?.let { textToMatch.add(it) }
        sbn.key?.let { textToMatch.add(it) }

        // Also, include userId or package info
        textToMatch.add("user_${sbn.userId}")

        // Let's normalize and check for a match
        // Normalization: strip spaces, dashes, and '+'
        val normalizedAllowed = allowedAccounts.map { it.replace(" ", "").replace("+", "").replace("-", "") }
        val normalizedHarvested = textToMatch.map { it.replace(" ", "").replace("+", "").replace("-", "") }

        for (acc in normalizedAllowed) {
            for (text in normalizedHarvested) {
                if (text.contains(acc, ignoreCase = true)) {
                    Log.d(TAG, "Confirmed: receiving notification matched allowed account $acc")
                    return true
                }
            }
        }

        // Extremely critical fallback: if we couldn't harvest ANY recipient-specific indicators, 
        // OR the harvested indices are completely void of any phone number or custom labels, 
        // we shouldn't block. This is because on devices running a single WhatsApp instance, 
        // no recipient subText/label is injected by OS or WhatsApp, so checking would always fail!
        // So, let's check if the harvested list has any digits or subtexts resembling phone numbers or labels.
        // If there's no dual identifier, we return true as a safe fallback.
        val hasAnyNumericMetadata = normalizedHarvested.any { text ->
            // Check if any harvested text contains a sequence of 5 or more digits (like a partial phone number or ID)
            text.any { it.isDigit() } && text.filter { it.isDigit() }.length >= 5
        }

        if (!hasAnyNumericMetadata) {
            Log.d(TAG, "No specific recipient phone Metadata found in notification. Safe fallback: permitting.")
            return true
        }

        Log.e(TAG, "Blocked notification: did not match any allowed receiving accounts ($allowedAccounts). Metadata fields obtained: $textToMatch")
        return false
    }

    private fun isRuleTargetAccountAllowed(sbn: StatusBarNotification, reply: CustomReplyEntity, title: String, rawText: String, settings: AppSettings): Boolean {
        if (!isAccountAllowed(sbn, settings)) return false

        val target = reply.targetAccount
        if (target.isNullOrBlank()) return true

        val normTarget = target.trim().replace(" ", "").replace("+", "").replace("-", "")
        
        // Match target against the harvested text for this notification
        val textToMatch = mutableListOf<String>()

        val extras = sbn.notification.extras
        if (extras != null) {
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.let { textToMatch.add(it) }
            extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.let { textToMatch.add(it) }
            try {
                for (key in extras.keySet()) {
                    if (key != Notification.EXTRA_TITLE && 
                        key != Notification.EXTRA_TEXT && 
                        key != "android.title" && 
                        key != "android.text" &&
                        key != "android.bigText") {
                        val value = extras.get(key)
                        if (value is CharSequence || value is String) {
                            textToMatch.add(value.toString())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading extras on target check: ${e.message}")
            }
        }

        sbn.tag?.let { textToMatch.add(it) }
        sbn.key?.let { textToMatch.add(it) }
        textToMatch.add("user_${sbn.userId}")

        val normalizedHarvested = textToMatch.map { it.replace(" ", "").replace("+", "").replace("-", "") }

        for (text in normalizedHarvested) {
            if (text.contains(normTarget, ignoreCase = true)) {
                return true
            }
        }
        
        return false
    }

    private fun replyToNotification(sbn: StatusBarNotification, replyMessage: String): Boolean {
        val actions = sbn.notification.actions ?: return false
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            for (remoteInput in remoteInputs) {
                if (remoteInput.resultKey != null) {
                    val results = Bundle()
                    results.putCharSequence(remoteInput.resultKey, replyMessage)

                    val intent = Intent()
                    RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, results)
                    RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)

                    try {
                        action.actionIntent.send(applicationContext, 0, intent)
                        return true
                    } catch (e: Exception) {
                        Log.e(TAG, "RemoteInput replication failed: ${e.message}", e)
                    }
                }
            }
        }
        return false
    }

    private fun isWithinWorkingHours(start: String, end: String): Boolean {
        val sdf = SimpleDateFormat("HH:mm", Locale.US)
        try {
            val now = Calendar.getInstance()
            val currentTimeStr = String.format("%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
            val dateNow = sdf.parse(currentTimeStr)
            val dateStart = sdf.parse(start)
            val dateEnd = sdf.parse(end)
            if (dateStart != null && dateEnd != null && dateNow != null) {
                if (dateStart.before(dateEnd)) {
                    return dateNow >= dateStart && dateNow <= dateEnd
                } else {
                    return dateNow >= dateStart || dateNow <= dateEnd
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed verifying working hours: ${e.message}")
        }
        return true
    }
}
