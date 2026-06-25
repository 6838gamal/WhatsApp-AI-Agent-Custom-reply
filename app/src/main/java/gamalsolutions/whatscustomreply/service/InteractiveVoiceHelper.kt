package gamalsolutions.whatscustomreply.service

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import gamalsolutions.whatscustomreply.BuildConfig
import gamalsolutions.whatscustomreply.data.datastore.AppSettings
import gamalsolutions.whatscustomreply.data.security.EncryptedPrefsManager
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import java.util.Locale
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InteractiveVoiceHelper(private val context: Context) : KoinComponent, TextToSpeech.OnInitListener {
    private val TAG = "InteractiveVoiceHelper"

    private val encryptedPrefs: EncryptedPrefsManager by inject()
    private val logsRepository: LogsRepository by inject()
    private val httpClient: OkHttpClient by inject()
    private val repliesRepository: RepliesRepository by inject()
    private val customApiRepository: CustomApiRepository by inject()

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    private var isListening = false
    private var audioManager: AudioManager? = null
    private var activeScope: CoroutineScope? = null
    private var mediaPlayer: android.media.MediaPlayer? = null
    
    private val conversationHistory = mutableListOf<JSONObject>()
    private var systemPrompt = ""
    private var callerNumber = ""
    private var appLanguage = "ar"
    private var currentSettings: AppSettings? = null
    
    fun startDialogue(settings: AppSettings, incomingNumber: String) {
        Log.d(TAG, "Starting interactive voice dialogue with: $incomingNumber")
        callerNumber = incomingNumber
        systemPrompt = settings.interactiveVoiceCallPrompt
        appLanguage = settings.appLanguage
        currentSettings = settings
        
        activeScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        activeScope?.launch {
            try {
                audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.isSpeakerphoneOn = true
            } catch (e: Exception) {
                Log.e(TAG, "Error setting audio manager config: ${e.message}")
            }

            // Initialize TextToSpeech on the main thread
            tts = TextToSpeech(context.applicationContext, this@InteractiveVoiceHelper)
            conversationHistory.clear()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = if (appLanguage == "en") Locale.US else Locale("ar")
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language code $appLanguage is not supported or missing data in TTS.")
                tts?.setLanguage(Locale.US)
            }
            
            try {
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)
                Log.d(TAG, "Successfully configured USAGE_VOICE_COMMUNICATION on TextToSpeech")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting AudioAttributes on TTS: ${e.message}")
            }

            isTtsReady = true
            
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS Started speaking: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS Finished speaking: $utteranceId")
                    activeScope?.launch(Dispatchers.Main) {
                        delay(200) // gentle padding before starting the recognizer
                        startListeningLoop()
                    }
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS Error speaking: $utteranceId")
                }
            })

            // Initial voice greeting invitation
            speakGreeting()
        } else {
            Log.e(TAG, "TTS Init failed with status: $status")
        }
    }

    private fun speakGreeting() {
        val voiceFile = java.io.File(context.filesDir, "voice_greeting.3gp")
        if (voiceFile.exists()) {
            Log.d(TAG, "Custom recorded voice greeting exists! Playing audio greeting file...")
            playAudioFile(voiceFile)
        } else {
            val greeting = if (!systemPrompt.isNullOrBlank()) {
                systemPrompt
            } else if (appLanguage == "en") {
                "Hello, I am the automated smartphone assistant. Please tell me how I can help you."
            } else {
                "مرحباً بك، أنا المساعد الهاتفي الذكي لصاحب هذا الهاتف. كيف يمكنني خدمتك ومساعدتك اليوم؟ تفضل بالتحدث وتوجيه رسالتك."
            }
            speakText(greeting)
        }
    }

    private fun playAudioFile(file: java.io.File) {
        val scope = activeScope ?: return
        scope.launch(Dispatchers.Main) {
            try {
                mediaPlayer?.release()
                mediaPlayer = android.media.MediaPlayer().apply {
                    setDataSource(file.absolutePath)
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    prepare()
                    start()
                    setOnCompletionListener {
                        Log.d(TAG, "Audio file greeting completed playing. Starting listening loop.")
                        release()
                        mediaPlayer = null
                        startListeningLoop()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing audio file greeting: ${e.message}. Falling back to TTS greeting.")
                val greeting = if (!systemPrompt.isNullOrBlank()) {
                    systemPrompt
                } else if (appLanguage == "en") {
                    "Hello, I am the automated smartphone assistant. Please tell me how I can help you."
                } else {
                    "مرحباً بك، أنا المساعد الهاتفي الذكي لصاحب هذا الهاتف. كيف يمكنني خدمتك ومساعدتك اليوم؟ تفضل بالتحدث وتوجيه رسالتك."
                }
                speakText(greeting)
            }
        }
    }

    private fun speakText(text: String) {
        if (!isTtsReady || tts == null) return
        val utteranceId = "InteractiveVoice_" + System.currentTimeMillis()
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL)
        }
        Log.d(TAG, "Speaking text: $text with stream STREAM_VOICE_CALL")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun startListeningLoop() {
        val scope = activeScope ?: return
        scope.launch(Dispatchers.Main) {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer ready for voice input...")
                        isListening = true
                    }

                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        Log.d(TAG, "SpeechRecognizer Error: $error. Restarting listener...")
                        isListening = false
                        activeScope?.launch(Dispatchers.Main) {
                            delay(1200) // standard timeout padding
                            if (activeScope != null) {
                                startListeningLoop()
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val callerSaidText = matches?.firstOrNull() ?: ""
                        Log.d(TAG, "Caller voice decoded: $callerSaidText")

                        if (callerSaidText.isNotBlank()) {
                            processInputAndReply(callerSaidText)
                        } else {
                            startListeningLoop()
                        }
                    }

                    override fun onPartialResults(results: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (appLanguage == "en") "en-US" else "ar-YE")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, if (appLanguage == "en") "en" else "ar")
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            try {
                speechRecognizer?.startListening(recognizerIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting SpeechRecognizer: ${e.message}")
            }
        }
    }

    private fun normalizeArabic(text: String): String {
        return text.trim()
            .replace("أ", "ا")
            .replace("إ", "ا")
            .replace("آ", "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace(Regex("[\u064B-\u0652]"), "") 
    }

    private data class VoiceReplyResult(val replyText: String, val mode: String)

    private fun processInputAndReply(text: String) {
        val scope = activeScope ?: return
        scope.launch(Dispatchers.Main) {
            val userTurn = JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", text)))
            }
            conversationHistory.add(userTurn)

            // Dynamic Voice Decision
            val voiceResult = decideVoiceReply(text)
            val replyText = voiceResult.replyText
            
            val modelTurn = JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().put("text", replyText)))
            }
            conversationHistory.add(modelTurn)

            // Log voice conversation log
            try {
                logsRepository.insertLog(
                    AutoReplyLogEntity(
                        senderName = callerNumber,
                        messageText = "🎙️ [صوتي الوارد]: $text",
                        replyText = replyText,
                        mode = "VOICE (${voiceResult.mode})",
                        isSuccess = true
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting interactive voice log: ${e.message}")
            }

            // Respond vocally to caller
            speakText(replyText)
        }
    }

    private suspend fun decideVoiceReply(textInput: String): VoiceReplyResult = withContext(Dispatchers.IO) {
        val normalizedSpoken = normalizeArabic(textInput)
        Log.d(TAG, "decideVoiceReply: normalized spoken input: $normalizedSpoken")
        
        // 1. Search custom replies for keyword matches
        try {
            val enabledReplies = repliesRepository.getEnabledReplies()
            for (reply in enabledReplies) {
                val kw = reply.keyword.trim()
                if (kw.isNotEmpty()) {
                    val normalizedKw = normalizeArabic(kw)
                    if (normalizedSpoken.contains(normalizedKw, ignoreCase = true)) {
                        Log.d(TAG, "Local keyword match found! Pattern: '$kw', Reply: '${reply.replyText}'")
                        return@withContext VoiceReplyResult(reply.replyText, "LOCAL_RULES")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error matching local replies in interactive voice: ${e.message}")
        }

        // 2. If no local match, but custom API is configured, call custom API!
        val settings = currentSettings
        if (settings != null && settings.apiUrl.isNotBlank()) {
            Log.d(TAG, "No local match. Forwarding voice input to Custom API: ${settings.apiUrl}")
            val apiResult = customApiRepository.generateReply(
                apiUrl = settings.apiUrl,
                apiMethod = settings.apiMethod,
                apiHeaders = settings.apiHeaders,
                apiBodyTemplate = settings.apiBodyTemplate,
                apiResponsePath = settings.apiResponsePath,
                sender = callerNumber,
                message = textInput
            )
            var apiReply: String? = null
            apiResult.onSuccess { text ->
                apiReply = text
            }.onFailure { e ->
                Log.e(TAG, "Custom API voice call failed: ${e.message}")
            }
            if (apiReply != null) {
                return@withContext VoiceReplyResult(apiReply!!, "CUSTOM_API")
            }
        }

        // 3. Fallback: If no local rules match and no Custom API is configured, use standard fallback message or custom prompt.
        val defaultText = if (appLanguage == "en") {
            "I could not match your request. Please mention a keyword like price or location, or leave a message."
        } else {
            "لم أستطع فهم طلبك بدقة، يرجى ذكر كلمة مفتاحية واضحة كالموقع، الأسعار أو ترك رسائل لخدمتكم."
        }
        
        return@withContext VoiceReplyResult(defaultText, "FALLBACK")
    }

    fun shutdown() {
        Log.d(TAG, "Stopping interactive voice dialogue.")
        val scope = activeScope
        activeScope = null
        scope?.cancel()
        
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            Log.e(TAG, "SpeechRecognizer destroy error: ${e.message}")
        }

        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e(TAG, "TTS shutdown error: ${e.message}")
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer shutdown error: ${e.message}")
        }

        try {
            audioManager?.mode = AudioManager.MODE_NORMAL
            audioManager?.isSpeakerphoneOn = false
        } catch (e: Exception) {
            Log.e(TAG, "AudioManager configurations recovery error: ${e.message}")
        }
    }
}
