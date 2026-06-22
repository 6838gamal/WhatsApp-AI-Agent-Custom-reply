package gamalsolutions.whatscustomreply.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class SpeechHelper(private val context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val pendingTexts = mutableListOf<String>()
    private var currentLanguage = "ar"

    fun speak(text: String, lang: String = "ar") {
        currentLanguage = lang
        synchronized(this) {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext, this)
            }
            if (isTtsInitialized) {
                speakText(text)
            } else {
                pendingTexts.add(text)
            }
        }
    }

    override fun onInit(status: Int) {
        synchronized(this) {
            if (status == TextToSpeech.SUCCESS) {
                val locale = if (currentLanguage == "en") Locale.US else Locale("ar")
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("SpeechHelper", "Language code $currentLanguage is not supported or missing data.")
                }
                isTtsInitialized = true
                for (pending in pendingTexts) {
                    speakText(pending)
                }
                pendingTexts.clear()
            } else {
                Log.e("SpeechHelper", "TTS Initialization failed with status $status")
            }
        }
    }

    private fun speakText(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "AutoReplySpeech_" + System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e("SpeechHelper", "Error speaking text via TTS: ${e.message}")
        }
    }

    fun shutdown() {
        synchronized(this) {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isTtsInitialized = false
        }
    }
}
