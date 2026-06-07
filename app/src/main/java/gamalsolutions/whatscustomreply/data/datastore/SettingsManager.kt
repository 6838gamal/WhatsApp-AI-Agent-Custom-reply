package gamalsolutions.whatscustomreply.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auto_reply_settings")

data class AppSettings(
    val ignoreGroups: Boolean,
    val ignoreDuplicates: Boolean,
    val replyOncePerUser: Boolean,
    val randomDelayEnabled: Boolean,
    val randomDelayMin: Int,
    val randomDelayMax: Int,
    val workingHoursEnabled: Boolean,
    val workingHoursStart: String,
    val workingHoursEnd: String,
    val isServiceEnabled: Boolean,
    val replyMode: String, // "CUSTOM", "GEMINI", "HYBRID"
    val geminiModel: String,
    val systemPrompt: String,
    val appLanguage: String
)

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_IGNORE_GROUPS = booleanPreferencesKey("ignore_groups")
        val KEY_IGNORE_DUPLICATES = booleanPreferencesKey("ignore_duplicates")
        val KEY_REPLY_ONCE_PER_USER = booleanPreferencesKey("reply_once_per_user")
        val KEY_RANDOM_DELAY_ENABLED = booleanPreferencesKey("random_delay_enabled")
        val KEY_RANDOM_DELAY_MIN = intPreferencesKey("random_delay_min")
        val KEY_RANDOM_DELAY_MAX = intPreferencesKey("random_delay_max")
        val KEY_WORKING_HOURS_ENABLED = booleanPreferencesKey("working_hours_enabled")
        val KEY_WORKING_HOURS_START = stringPreferencesKey("working_hours_start")
        val KEY_WORKING_HOURS_END = stringPreferencesKey("working_hours_end")
        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_REPLY_MODE = stringPreferencesKey("reply_mode")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_GEMINI_SYSTEM_PROMPT = stringPreferencesKey("gemini_system_prompt")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { preferences ->
        AppSettings(
            ignoreGroups = preferences[KEY_IGNORE_GROUPS] ?: true,
            ignoreDuplicates = preferences[KEY_IGNORE_DUPLICATES] ?: true,
            replyOncePerUser = preferences[KEY_REPLY_ONCE_PER_USER] ?: false,
            randomDelayEnabled = preferences[KEY_RANDOM_DELAY_ENABLED] ?: false,
            randomDelayMin = preferences[KEY_RANDOM_DELAY_MIN] ?: 2,
            randomDelayMax = preferences[KEY_RANDOM_DELAY_MAX] ?: 7,
            workingHoursEnabled = preferences[KEY_WORKING_HOURS_ENABLED] ?: false,
            workingHoursStart = preferences[KEY_WORKING_HOURS_START] ?: "09:00",
            workingHoursEnd = preferences[KEY_WORKING_HOURS_END] ?: "18:00",
            isServiceEnabled = preferences[KEY_SERVICE_ENABLED] ?: true,
            replyMode = preferences[KEY_REPLY_MODE] ?: "CUSTOM",
            geminiModel = preferences[KEY_GEMINI_MODEL] ?: "gemini-2.5-flash",
            systemPrompt = preferences[KEY_GEMINI_SYSTEM_PROMPT] ?: "You are an automated WhatsApp helper. Draft a short, concise, polite, and helpful answer.",
            appLanguage = preferences[KEY_APP_LANGUAGE] ?: "ar" // Default to Arabic as requested by user's prompt language
        )
    }

    suspend fun updateIgnoreGroups(value: Boolean) = set(KEY_IGNORE_GROUPS, value)
    suspend fun updateIgnoreDuplicates(value: Boolean) = set(KEY_IGNORE_DUPLICATES, value)
    suspend fun updateReplyOncePerUser(value: Boolean) = set(KEY_REPLY_ONCE_PER_USER, value)
    suspend fun updateRandomDelayEnabled(value: Boolean) = set(KEY_RANDOM_DELAY_ENABLED, value)
    suspend fun updateRandomDelayMin(value: Int) = set(KEY_RANDOM_DELAY_MIN, value)
    suspend fun updateRandomDelayMax(value: Int) = set(KEY_RANDOM_DELAY_MAX, value)
    suspend fun updateWorkingHoursEnabled(value: Boolean) = set(KEY_WORKING_HOURS_ENABLED, value)
    suspend fun updateWorkingHoursStart(value: String) = set(KEY_WORKING_HOURS_START, value)
    suspend fun updateWorkingHoursEnd(value: String) = set(KEY_WORKING_HOURS_END, value)
    suspend fun updateServiceEnabled(value: Boolean) = set(KEY_SERVICE_ENABLED, value)
    suspend fun updateReplyMode(value: String) = set(KEY_REPLY_MODE, value)
    suspend fun updateGeminiModel(value: String) = set(KEY_GEMINI_MODEL, value)
    suspend fun updateSystemPrompt(value: String) = set(KEY_GEMINI_SYSTEM_PROMPT, value)
    suspend fun updateAppLanguage(value: String) = set(KEY_APP_LANGUAGE, value)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
