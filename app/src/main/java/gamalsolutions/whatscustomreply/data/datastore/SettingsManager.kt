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
    val replyMode: String, // "CUSTOM", "API", "HYBRID"
    val apiUrl: String,
    val apiMethod: String,
    val apiHeaders: String,
    val apiBodyTemplate: String,
    val apiResponsePath: String,
    val appLanguage: String,
    val callReplyEnabled: Boolean,
    val callReplyText: String,
    val ringerVolume: Int,
    val mediaVolume: Int,
    val ringerMode: Int,
    val dismissNotificationsEnabled: Boolean,
    val voiceReplyEnabled: Boolean,
    val primaryAccountPhone: String,
    val additionalAccountPhones: String,
    val interactiveVoiceCallEnabled: Boolean,
    val interactiveVoiceCallPrompt: String
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
        val KEY_API_URL = stringPreferencesKey("api_url")
        val KEY_API_METHOD = stringPreferencesKey("api_method")
        val KEY_API_HEADERS = stringPreferencesKey("api_headers")
        val KEY_API_BODY_TEMPLATE = stringPreferencesKey("api_body_template")
        val KEY_API_RESPONSE_PATH = stringPreferencesKey("api_response_path")
        val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        val KEY_CALL_REPLY_ENABLED = booleanPreferencesKey("call_reply_enabled")
        val KEY_CALL_REPLY_TEXT = stringPreferencesKey("call_reply_text")
        val KEY_RINGER_VOLUME = intPreferencesKey("ringer_volume")
        val KEY_MEDIA_VOLUME = intPreferencesKey("media_volume")
        val KEY_RINGER_MODE = intPreferencesKey("ringer_mode")
        val KEY_DISMISS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("dismiss_notifications_enabled")
        val KEY_VOICE_REPLY_ENABLED = booleanPreferencesKey("voice_reply_enabled")
        val KEY_PRIMARY_ACCOUNT_PHONE = stringPreferencesKey("primary_account_phone")
        val KEY_ADDITIONAL_ACCOUNT_PHONES = stringPreferencesKey("additional_account_phones")
        val KEY_INTERACTIVE_VOICE_CALL_ENABLED = booleanPreferencesKey("interactive_voice_call_enabled")
        val KEY_INTERACTIVE_VOICE_CALL_PROMPT = stringPreferencesKey("interactive_voice_call_prompt")
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
            apiUrl = preferences[KEY_API_URL] ?: "https://api.example.com/reply",
            apiMethod = preferences[KEY_API_METHOD] ?: "POST",
            apiHeaders = preferences[KEY_API_HEADERS] ?: "Content-Type: application/json\nAuthorization: Bearer your-token-here",
            apiBodyTemplate = preferences[KEY_API_BODY_TEMPLATE] ?: "{\n  \"sender\": \"{sender}\",\n  \"message\": \"{message}\"\n}",
            apiResponsePath = preferences[KEY_API_RESPONSE_PATH] ?: "reply",
            appLanguage = preferences[KEY_APP_LANGUAGE] ?: "ar",
            callReplyEnabled = preferences[KEY_CALL_REPLY_ENABLED] ?: false,
            callReplyText = preferences[KEY_CALL_REPLY_TEXT] ?: "مرحباً، أنا غير متاح حالياً بالاتصال. سأتواصل معك فور تفرغي.",
            ringerVolume = preferences[KEY_RINGER_VOLUME] ?: 70,
            mediaVolume = preferences[KEY_MEDIA_VOLUME] ?: 60,
            ringerMode = preferences[KEY_RINGER_MODE] ?: 2, // Normal (0 = Silent, 1 = Vibrate, 2 = Normal)
            dismissNotificationsEnabled = preferences[KEY_DISMISS_NOTIFICATIONS_ENABLED] ?: false,
            voiceReplyEnabled = preferences[KEY_VOICE_REPLY_ENABLED] ?: false,
            primaryAccountPhone = preferences[KEY_PRIMARY_ACCOUNT_PHONE] ?: "",
            additionalAccountPhones = preferences[KEY_ADDITIONAL_ACCOUNT_PHONES] ?: "",
            interactiveVoiceCallEnabled = preferences[KEY_INTERACTIVE_VOICE_CALL_ENABLED] ?: false,
            interactiveVoiceCallPrompt = preferences[KEY_INTERACTIVE_VOICE_CALL_PROMPT] ?: "مرحباً، أنا المساعد الذكي لصاحب هذا الهاتف. إنه غير متاح حالياً للرد على المكالمات، وهو يثق بي للرد عليك والتجاوب معك بالكامل ومساعدتك وتسجيل طلبك. تفضل، كيف يمكنني خدمتك ومساعدتك اليوم؟"
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
    suspend fun updateApiUrl(value: String) = set(KEY_API_URL, value)
    suspend fun updateApiMethod(value: String) = set(KEY_API_METHOD, value)
    suspend fun updateApiHeaders(value: String) = set(KEY_API_HEADERS, value)
    suspend fun updateApiBodyTemplate(value: String) = set(KEY_API_BODY_TEMPLATE, value)
    suspend fun updateApiResponsePath(value: String) = set(KEY_API_RESPONSE_PATH, value)
    suspend fun updateAppLanguage(value: String) = set(KEY_APP_LANGUAGE, value)
    suspend fun updateCallReplyEnabled(value: Boolean) = set(KEY_CALL_REPLY_ENABLED, value)
    suspend fun updateCallReplyText(value: String) = set(KEY_CALL_REPLY_TEXT, value)
    suspend fun updateRingerVolume(value: Int) = set(KEY_RINGER_VOLUME, value)
    suspend fun updateMediaVolume(value: Int) = set(KEY_MEDIA_VOLUME, value)
    suspend fun updateRingerMode(value: Int) = set(KEY_RINGER_MODE, value)
    suspend fun updateDismissNotificationsEnabled(value: Boolean) = set(KEY_DISMISS_NOTIFICATIONS_ENABLED, value)
    suspend fun updateVoiceReplyEnabled(value: Boolean) = set(KEY_VOICE_REPLY_ENABLED, value)
    suspend fun updatePrimaryAccountPhone(value: String) = set(KEY_PRIMARY_ACCOUNT_PHONE, value)
    suspend fun updateAdditionalAccountPhones(value: String) = set(KEY_ADDITIONAL_ACCOUNT_PHONES, value)
    suspend fun updateInteractiveVoiceCallEnabled(value: Boolean) = set(KEY_INTERACTIVE_VOICE_CALL_ENABLED, value)
    suspend fun updateInteractiveVoiceCallPrompt(value: String) = set(KEY_INTERACTIVE_VOICE_CALL_PROMPT, value)

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
