package gamalsolutions.whatscustomreply.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.whatscustomreply.data.api.GeminiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import gamalsolutions.whatscustomreply.data.datastore.AppSettings
import gamalsolutions.whatscustomreply.data.datastore.SettingsManager
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import gamalsolutions.whatscustomreply.data.security.EncryptedPrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repliesRepository: RepliesRepository,
    private val logsRepository: LogsRepository,
    val geminiRepository: GeminiRepository,
    private val settingsManager: SettingsManager,
    private val encryptedPrefs: EncryptedPrefsManager
) : ViewModel() {

    // Custom Replies State Flow
    val replies: StateFlow<List<CustomReplyEntity>> = repliesRepository.allReplies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Message / Reply Logs State Flow
    val logs: StateFlow<List<AutoReplyLogEntity>> = logsRepository.allLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLogCount: StateFlow<Int> = logsRepository.logCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successLogCount: StateFlow<Int> = logsRepository.successCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Reactive Settings State
    val settings: StateFlow<AppSettings> = settingsManager.settingsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSettings(
                ignoreGroups = true,
                ignoreDuplicates = true,
                replyOncePerUser = false,
                randomDelayEnabled = false,
                randomDelayMin = 2,
                randomDelayMax = 7,
                workingHoursEnabled = false,
                workingHoursStart = "09:00",
                workingHoursEnd = "18:00",
                isServiceEnabled = true,
                replyMode = "CUSTOM",
                geminiModel = "gemini-2.5-flash",
                systemPrompt = "You are an automated WhatsApp helper. Draft a short, concise, polite, and helpful answer.",
                appLanguage = "ar"
            )
        )

    // Secure SharedPreferences wrapper for UI API key
    private val _geminiApiKey = MutableStateFlow(encryptedPrefs.getGeminiApiKey())
    val geminiApiKey = _geminiApiKey.asStateFlow()

    // Shared prefill state for contact replies shortcut from Logs Screen
    private val _prefilledContact = MutableStateFlow<String?>(null)
    val prefilledContact: StateFlow<String?> = _prefilledContact.asStateFlow()

    fun setPrefilledContact(contactName: String?) {
        _prefilledContact.value = contactName
    }

    fun clearPrefilledContact() {
        _prefilledContact.value = null
    }

    fun updateGeminiApiKey(apiKey: String) {
        encryptedPrefs.saveGeminiApiKey(apiKey)
        _geminiApiKey.value = apiKey
    }

    // Gemini connection test UI state
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult = _testConnectionResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    // --- Custom Reply CRUD Actions ---
    fun addReply(keyword: String, replyText: String, contactName: String? = null) {
        viewModelScope.launch {
            repliesRepository.insertReply(
                CustomReplyEntity(keyword = keyword, replyText = replyText, isEnabled = true, contactName = contactName)
            )
        }
    }

    fun updateReply(reply: CustomReplyEntity) {
        viewModelScope.launch {
            repliesRepository.updateReply(reply)
        }
    }

    fun deleteReply(reply: CustomReplyEntity) {
        viewModelScope.launch {
            repliesRepository.deleteReply(reply)
        }
    }

    fun toggleReplyCode(reply: CustomReplyEntity, isEnabled: Boolean) {
        viewModelScope.launch {
            repliesRepository.updateReply(reply.copy(isEnabled = isEnabled))
        }
    }

    // --- Manual logging & clear ---
    fun clearLogs() {
        viewModelScope.launch {
            logsRepository.clearAllLogs()
        }
    }

    fun insertLog(log: AutoReplyLogEntity) {
        viewModelScope.launch {
            logsRepository.insertLog(log)
        }
    }

    // --- Datastore Settings update triggers ---
    fun updateIgnoreGroups(value: Boolean) {
        viewModelScope.launch { settingsManager.updateIgnoreGroups(value) }
    }

    fun updateIgnoreDuplicates(value: Boolean) {
        viewModelScope.launch { settingsManager.updateIgnoreDuplicates(value) }
    }

    fun updateReplyOncePerUser(value: Boolean) {
        viewModelScope.launch { settingsManager.updateReplyOncePerUser(value) }
    }

    fun updateRandomDelayEnabled(value: Boolean) {
        viewModelScope.launch { settingsManager.updateRandomDelayEnabled(value) }
    }

    fun updateRandomDelayMin(value: Int) {
        viewModelScope.launch { settingsManager.updateRandomDelayMin(value) }
    }

    fun updateRandomDelayMax(value: Int) {
        viewModelScope.launch { settingsManager.updateRandomDelayMax(value) }
    }

    fun updateWorkingHoursEnabled(value: Boolean) {
        viewModelScope.launch { settingsManager.updateWorkingHoursEnabled(value) }
    }

    fun updateWorkingHoursStart(value: String) {
        viewModelScope.launch { settingsManager.updateWorkingHoursStart(value) }
    }

    fun updateWorkingHoursEnd(value: String) {
        viewModelScope.launch { settingsManager.updateWorkingHoursEnd(value) }
    }

    fun updateServiceEnabled(value: Boolean) {
        viewModelScope.launch { settingsManager.updateServiceEnabled(value) }
    }

    fun updateReplyMode(value: String) {
        viewModelScope.launch { settingsManager.updateReplyMode(value) }
    }

    fun updateGeminiModel(value: String) {
        viewModelScope.launch { settingsManager.updateGeminiModel(value) }
    }

    fun updateSystemPrompt(value: String) {
        viewModelScope.launch { settingsManager.updateSystemPrompt(value) }
    }

    fun updateAppLanguage(value: String) {
        viewModelScope.launch { settingsManager.updateAppLanguage(value) }
    }

    // --- Gemini Connection Testing ---
    fun testGeminiConnection() {
        viewModelScope.launch {
            _isTestingConnection.value = true
            _testConnectionResult.value = "Connecting to Gemini..."
            val result = geminiRepository.generateReply(
                prompt = "Please reply with exactly: 'API Connection Successful!' to verify everything works.",
                systemPrompt = "You are testing the raw API connectivity of a user's key.",
                model = settings.value.geminiModel
            )
            _isTestingConnection.value = false
            result.onSuccess { text ->
                _testConnectionResult.value = "Success: $text"
            }.onFailure { e ->
                _testConnectionResult.value = "Failed: ${e.message}"
            }
        }
    }

    fun resetConnectionTestResult() {
        _testConnectionResult.value = null
    }
}
