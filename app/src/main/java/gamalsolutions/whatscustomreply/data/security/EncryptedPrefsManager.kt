package gamalsolutions.whatscustomreply.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class EncryptedPrefsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_settings",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    fun saveGeminiApiKey(apiKey: String) {
        sharedPrefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    fun getGeminiApiKey(): String {
        return sharedPrefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
    }
}
