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
    private val geminiRepository: gamalsolutions.whatscustomreply.data.api.GeminiRepository by inject()
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

            // 1. Target Scope Filtering (Individual, Groups, or Both)
            val isGroup = extras.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false) ||
                          extras.getBoolean("android.isGroupConversation", false) ||
                          (extras.getCharSequence("android.conversationTitle") != null)
            val scope = settings.replyTargetScope
            val shouldSkip = when (scope) {
                "INDIVIDUAL" -> isGroup
                "GROUPS" -> !isGroup
                "BOTH" -> false
                else -> false
            }
            if (shouldSkip) {
                Log.d(TAG, "Skipping notification (isGroup=$isGroup) because replyTargetScope is $scope")
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
        var replyText: String? = null
        var errorMessage: String? = null
        val logMode = "GEMINI_AI"

        val geminiResult = geminiRepository.generateReply(
            apiKey = settings.geminiApiKey,
            systemInstruction = settings.geminiSystemInstruction,
            message = message
        )

        geminiResult.onSuccess { text ->
            replyText = text
        }.onFailure { e ->
            Log.e(TAG, "Gemini auto-reply failed: ${e.message}", e)
            errorMessage = e.message ?: e.toString()
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
                replyText = replyText ?: "Failed: ${errorMessage ?: "Unknown error"}",
                mode = logMode,
                isSuccess = isSuccess
            )
        )

        if (isSuccess && replyText != null) {
            repliedUsers.add(sender)
            Log.d(TAG, "Automated Gemini reply sent successfully using RemoteInput!")
            if (settings.voiceReplyEnabled) {
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
        return true
    }

    private fun isRuleTargetAccountAllowed(sbn: StatusBarNotification, reply: CustomReplyEntity, title: String, rawText: String, settings: AppSettings): Boolean {
        return true
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
