package gamalsolutions.whatscustomreply.service

import android.app.Notification
import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import gamalsolutions.whatscustomreply.data.api.GeminiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
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
    private val geminiRepository: GeminiRepository by inject()
    private val settingsManager: gamalsolutions.whatscustomreply.data.datastore.SettingsManager by inject()

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

            // Exclude WhatsApp system/call notifications
            if (rawText.contains("Missed voice call") || rawText.contains("Missed video call") || rawText.contains("Incoming call")) {
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
        var mode = settings.replyMode // "CUSTOM", "GEMINI", "HYBRID"
        var replyText: String? = null
        var logMode = "CUSTOM"

        // Search Custom Keywords
        val matchedCustom = findCustomReplyMatch(message)

        if (mode == "CUSTOM") {
            replyText = matchedCustom
            logMode = "CUSTOM"
        } else if (mode == "GEMINI") {
            logMode = "GEMINI"
            val apiResult = geminiRepository.generateReply(
                prompt = "Sender: $sender\nMessage: $message",
                systemPrompt = settings.systemPrompt,
                model = settings.geminiModel
            )
            apiResult.onSuccess { text -> replyText = text }
            apiResult.onFailure { e -> Log.e(TAG, "Gemini reply generation failed: ${e.message}") }
        } else if (mode == "HYBRID") {
            if (matchedCustom != null) {
                replyText = matchedCustom
                logMode = "CUSTOM (HYBRID)"
            } else {
                logMode = "GEMINI (HYBRID)"
                val apiResult = geminiRepository.generateReply(
                    prompt = "Sender: $sender\nMessage: $message",
                    systemPrompt = settings.systemPrompt,
                    model = settings.geminiModel
                )
                apiResult.onSuccess { text -> replyText = text }
                apiResult.onFailure { e -> Log.e(TAG, "Gemini hybrid reply failed: ${e.message}") }
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
        }
    }

    private suspend fun findCustomReplyMatch(messageText: String): String? {
        val enabledReplies = repliesRepository.getEnabledReplies()
        for (reply in enabledReplies) {
            if (messageText.contains(reply.keyword, ignoreCase = true)) {
                return reply.replyText
            }
        }
        return null
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
