package gamalsolutions.whatscustomreply.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.datastore.SettingsManager
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PhoneCallReceiver : BroadcastReceiver(), KoinComponent {
    private val settingsManager: SettingsManager by inject()
    private val logsRepository: LogsRepository by inject()
    private val customApiRepository: CustomApiRepository by inject()

    companion object {
        private const val TAG = "PhoneCallReceiver"
        private var originalRingerMode: Int? = null
        private var originalRingerVolume: Int? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""

            Log.d(TAG, "Phone State changed: $state, Number: $incomingNumber")

            CoroutineScope(Dispatchers.IO).launch {
                val settings = settingsManager.settingsFlow.firstOrNull() ?: return@launch
                if (!settings.isServiceEnabled) return@launch

                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

                if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (originalRingerMode == null) {
                        originalRingerMode = audioManager.ringerMode
                    }
                    if (originalRingerVolume == null) {
                        originalRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
                    }

                    // 1. Dynamic Audio & Volume Handling
                    try {
                        // Set ringer mode first
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

                    // 2. Call Auto reply if enabled
                    if (settings.callReplyEnabled && incomingNumber.isNotBlank()) {
                        try {
                            var finalReplyText = settings.callReplyText
                            var usedMode = "CALL_RESPONDER (STATIC)"

                            if (settings.replyMode == "API" || settings.replyMode == "HYBRID") {
                                val apiResult = customApiRepository.generateReply(
                                    apiUrl = settings.apiUrl,
                                    apiMethod = settings.apiMethod,
                                    apiHeaders = settings.apiHeaders,
                                    apiBodyTemplate = settings.apiBodyTemplate,
                                    apiResponsePath = settings.apiResponsePath,
                                    sender = incomingNumber,
                                    message = "Incoming Call (Phone)"
                                )
                                apiResult.onSuccess { text ->
                                    finalReplyText = text
                                    usedMode = "CALL_RESPONDER (API)"
                                }.onFailure { e ->
                                    Log.e(TAG, "API call response extraction failed, falling back to static response text", e)
                                    finalReplyText = "${settings.callReplyText} (API Error: ${e.localizedMessage ?: e.message})"
                                    usedMode = "CALL_RESPONDER (API_FALLBACK)"
                                }
                            }

                            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                context.getSystemService(SmsManager::class.java)
                            } else {
                                @Suppress("DEPRECATION")
                                SmsManager.getDefault()
                            }

                            smsManager.sendTextMessage(incomingNumber, null, finalReplyText, null, null)

                            logsRepository.insertLog(
                                AutoReplyLogEntity(
                                    senderName = incomingNumber,
                                    messageText = "Incoming call (Phone Call)",
                                    replyText = finalReplyText,
                                    mode = usedMode,
                                    isSuccess = true
                                )
                            )
                            Log.d(TAG, "Call auto-reply SMS sent successfully to: $incomingNumber")
                        } catch (e: Exception) {
                            Log.e(TAG, "Call auto-reply SMS send failed", e)
                            try {
                                logsRepository.insertLog(
                                    AutoReplyLogEntity(
                                        senderName = incomingNumber,
                                        messageText = "Incoming call (Phone Call)",
                                        replyText = "${settings.callReplyText} (Failed: ${e.localizedMessage ?: e.message})",
                                        mode = "CALL_RESPONDER_FAILED",
                                        isSuccess = false
                                    )
                                )
                            } catch (logEx: Exception) {
                                Log.e(TAG, "Log database insertion failed", logEx)
                            }
                        }
                    }
                } else if (state == TelephonyManager.EXTRA_STATE_IDLE || state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
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
                }
            }
        }
    }
}
