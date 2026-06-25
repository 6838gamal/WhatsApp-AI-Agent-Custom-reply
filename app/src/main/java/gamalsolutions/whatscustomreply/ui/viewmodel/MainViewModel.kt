package gamalsolutions.whatscustomreply.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import gamalsolutions.whatscustomreply.data.api.CustomApiRepository
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import gamalsolutions.whatscustomreply.data.datastore.AppSettings
import gamalsolutions.whatscustomreply.data.datastore.SettingsManager
import gamalsolutions.whatscustomreply.data.repository.LogsRepository
import gamalsolutions.whatscustomreply.data.repository.RepliesRepository
import gamalsolutions.whatscustomreply.data.security.EncryptedPrefsManager
import gamalsolutions.whatscustomreply.data.repository.BusinessIntelligenceEngine
import gamalsolutions.whatscustomreply.data.repository.SystemEventsRepository
import gamalsolutions.whatscustomreply.data.database.SystemEventEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainViewModel(
    private val repliesRepository: RepliesRepository,
    private val logsRepository: LogsRepository,
    val customApiRepository: CustomApiRepository,
    private val settingsManager: SettingsManager,
    private val encryptedPrefs: EncryptedPrefsManager,
    val systemEventsRepository: SystemEventsRepository
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

    // System Events State Flow
    val systemEvents: StateFlow<List<SystemEventEntity>> = systemEventsRepository.allEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val systemEventsCount: StateFlow<Int> = systemEventsRepository.eventCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Reactively mapped Business Intelligence Dashboard State Flow
    val biDashboardData: StateFlow<BusinessIntelligenceEngine.BIDashboardData> = logs
        .map { logList -> BusinessIntelligenceEngine.analyzeLogs(logList) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessIntelligenceEngine.BIDashboardData())

    // Realtime WebSocket Monitoring state
    private val _webSocketStatus = MutableStateFlow("CONNECTED") // CONNECTING, CONNECTED, DISCONNECTED
    val webSocketStatus: StateFlow<String> = _webSocketStatus.asStateFlow()

    private val _webSocketEvents = MutableStateFlow<List<String>>(
        listOf(
            "WebSocket: Initialized connection with local interface",
            "WebSocket: Handshake succeeded with WS gateway",
            "WebSocket: Real-time intelligence monitoring listening..."
        )
    )
    val webSocketEvents: StateFlow<List<String>> = _webSocketEvents.asStateFlow()

    fun appendWebSocketLog(message: String) {
        val currentList = _webSocketEvents.value.toMutableList()
        currentList.add(0, "WS [${System.currentTimeMillis()}]: $message")
        _webSocketEvents.value = currentList.take(40) // Keep last 40 entries
    }

    fun toggleWebSocketStatus() {
        _webSocketStatus.value = when (_webSocketStatus.value) {
            "CONNECTED" -> "DISCONNECTED"
            "DISCONNECTED" -> "CONNECTING"
            else -> "CONNECTED"
        }
        appendWebSocketLog("WebSocket Connection state updated manually to: ${_webSocketStatus.value}")
    }

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
                apiUrl = "https://api.example.com/reply",
                apiMethod = "POST",
                apiHeaders = "Content-Type: application/json\nAuthorization: Bearer your-token-here",
                apiBodyTemplate = "{\n  \"sender\": \"{sender}\",\n  \"message\": \"{message}\"\n}",
                apiResponsePath = "reply",
                appLanguage = "ar",
                callReplyEnabled = false,
                callReplyText = "مرحباً، أنا غير متاح حالياً بالاتصال. سأتواصل معك فور تفرغي.",
                ringerVolume = 70,
                mediaVolume = 60,
                ringerMode = 2,
                dismissNotificationsEnabled = false,
                voiceReplyEnabled = false,
                primaryAccountPhone = "",
                additionalAccountPhones = "",
                interactiveVoiceCallEnabled = false,
                interactiveVoiceCallPrompt = "مرحباً، أنا المساعد الذكي لصاحب هذا الهاتف. إنه غير متاح حالياً للرد على المكالمات، وهو يثق بي للرد عليك والتجاوب معك بالكامل ومساعدتك وتسجيل طلبك. تفضل، كيف يمكنني خدمتك ومساعدتك اليوم؟"
            )
        )

    // Shared prefill state for contact replies shortcut from Logs Screen
    private val _prefilledContact = MutableStateFlow<String?>(null)
    val prefilledContact: StateFlow<String?> = _prefilledContact.asStateFlow()

    fun setPrefilledContact(contactName: String?) {
        _prefilledContact.value = contactName
    }

    fun clearPrefilledContact() {
        _prefilledContact.value = null
    }

    // API connection test UI state
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult = _testConnectionResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection = _isTestingConnection.asStateFlow()

    // Global application errors flow
    private val _appError = MutableStateFlow<String?>(null)
    val appError = _appError.asStateFlow()

    fun dismissAppError() {
        _appError.value = null
    }

    private fun launchSafe(contextAr: String, contextEn: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error during $contextEn: ${e.message}", e)
                val resolvedText = if (settings.value.appLanguage == "en") {
                    "Error ($contextEn): ${e.localizedMessage ?: e.message}"
                } else {
                    "خطأ ($contextAr): ${e.localizedMessage ?: e.message}"
                }
                _appError.value = resolvedText
            }
        }
    }

    // --- Custom Reply CRUD Actions ---
    fun addReply(
        keyword: String,
        replyText: String,
        contactName: String? = null,
        triggerType: String = "CHAT",
        replyType: String = "TEXT",
        targetAccount: String? = null
    ) {
        launchSafe("إضافة رد مخصص", "adding reply rule") {
            repliesRepository.insertReply(
                CustomReplyEntity(
                    keyword = keyword,
                    replyText = replyText,
                    isEnabled = true,
                    contactName = contactName,
                    triggerType = triggerType,
                    replyType = replyType,
                    targetAccount = targetAccount
                )
            )
            
            // Record Audit Trail
            systemEventsRepository.recordEvent(
                eventType = "AUDIT",
                eventCategory = "SECURITY",
                entityType = "RULE",
                entityId = keyword,
                customerId = "",
                conversationId = "",
                message = "إضافة قاعدة رد تلقائي للكلمة المفتاحية: \"$keyword\"",
                metadata = "{\"keyword\":\"$keyword\",\"replyText\":\"$replyText\",\"triggerType\":\"$triggerType\"}"
            )
            appendWebSocketLog("Audit: Added auto-reply rule for '$keyword'")
        }
    }

    fun updateReply(reply: CustomReplyEntity) {
        launchSafe("تحديث الرد المخصص", "updating reply rule") {
            repliesRepository.updateReply(reply)
            
            // Record Audit Trail
            systemEventsRepository.recordEvent(
                eventType = "AUDIT",
                eventCategory = "SECURITY",
                entityType = "RULE",
                entityId = reply.id.toString(),
                customerId = reply.contactName ?: "",
                conversationId = "",
                message = "تحديث قاعدة الرد للكلمة المفتاحية: \"${reply.keyword}\"",
                metadata = "{\"keyword\":\"${reply.keyword}\",\"isEnabled\":${reply.isEnabled},\"triggerType\":\"${reply.triggerType}\"}"
            )
            appendWebSocketLog("Audit: Updated auto-reply rule '${reply.keyword}'")
        }
    }

    fun deleteReply(reply: CustomReplyEntity) {
        launchSafe("حذف الرد المخصص", "deleting reply rule") {
            repliesRepository.deleteReply(reply)
            
            // Record Audit Trail
            systemEventsRepository.recordEvent(
                eventType = "AUDIT",
                eventCategory = "SECURITY",
                entityType = "RULE",
                entityId = reply.id.toString(),
                customerId = reply.contactName ?: "",
                conversationId = "",
                message = "حذف قاعدة الرد للكلمة المفتاحية: \"${reply.keyword}\"",
                metadata = "{\"keyword\":\"${reply.keyword}\"}"
            )
            appendWebSocketLog("Audit: Deleted auto-reply rule '${reply.keyword}'")
        }
    }

    fun toggleReplyCode(reply: CustomReplyEntity, isEnabled: Boolean) {
        launchSafe("تعديل حالة التفعيل", "toggling reply rule status") {
            repliesRepository.updateReply(reply.copy(isEnabled = isEnabled))
            
            // Record Audit Trail
            systemEventsRepository.recordEvent(
                eventType = "AUDIT",
                eventCategory = "SECURITY",
                entityType = "RULE",
                entityId = reply.id.toString(),
                customerId = "",
                conversationId = "",
                message = "تغيير حالة تفعيل قاعدة الرد \"${reply.keyword}\" إلى ${if (isEnabled) "نشط" else "معطل"}",
                metadata = "{\"keyword\":\"${reply.keyword}\",\"newStatus\":$isEnabled}"
            )
            appendWebSocketLog("Audit: Toggled rule '${reply.keyword}' to $isEnabled")
        }
    }

    // --- Manual logging & clear ---
    fun clearLogs() {
        launchSafe("مسح سجلات الردود", "clearing reply logs") {
            logsRepository.clearAllLogs()
            systemEventsRepository.recordEvent(
                eventType = "AUDIT",
                eventCategory = "SYSTEM",
                entityType = "LOGS",
                entityId = "ALL",
                customerId = "",
                conversationId = "",
                message = "مسح جميع سجلات الردود التلقائية بالكامل من النظام"
            )
            appendWebSocketLog("System: All auto-reply logs cleared")
        }
    }

    fun clearSystemEvents() {
        launchSafe("مسح سجلات الأحداث", "clearing system events") {
            systemEventsRepository.clearAllEvents()
            appendWebSocketLog("System: All system events cleared")
        }
    }

    fun insertLog(log: AutoReplyLogEntity) {
        launchSafe("حفظ سجل الرد", "inserting auto-reply log") {
            logsRepository.insertLog(log)
            appendWebSocketLog("Received message from ${log.senderName}: \"${log.messageText}\"")
        }
    }

    // --- Datastore Settings update triggers ---
    fun updateIgnoreGroups(value: Boolean) {
        launchSafe("تحديث تصفية المجموعات", "updating group ignore setting") { settingsManager.updateIgnoreGroups(value) }
    }

    fun updateIgnoreDuplicates(value: Boolean) {
        launchSafe("تحديث تصفية التكرار", "updating duplicate ignore setting") { settingsManager.updateIgnoreDuplicates(value) }
    }

    fun updateReplyOncePerUser(value: Boolean) {
        launchSafe("تحديث الرد لمرة واحدة", "updating single reply setting") { settingsManager.updateReplyOncePerUser(value) }
    }

    fun updateRandomDelayEnabled(value: Boolean) {
        launchSafe("تعديل تشغيل ميزة تأخير الرد", "updating random delay toggle") { settingsManager.updateRandomDelayEnabled(value) }
    }

    fun updateRandomDelayMin(value: Int) {
        launchSafe("تحديث الحد الأدنى لتأخير الرد", "updating min delay value") { settingsManager.updateRandomDelayMin(value) }
    }

    fun updateRandomDelayMax(value: Int) {
        launchSafe("تحديث الحد الأقصى لتأخير الرد", "updating max delay value") { settingsManager.updateRandomDelayMax(value) }
    }

    fun updateWorkingHoursEnabled(value: Boolean) {
        launchSafe("تعديل تفعيل أوقات العمل", "updating working hours toggle") { settingsManager.updateWorkingHoursEnabled(value) }
    }

    fun updateWorkingHoursStart(value: String) {
        launchSafe("تعديل وقت بدء ساعات العمل", "updating working hours start") { settingsManager.updateWorkingHoursStart(value) }
    }

    fun updateWorkingHoursEnd(value: String) {
        launchSafe("تعديل وقت انتهاء ساعات العمل", "updating working hours end") { settingsManager.updateWorkingHoursEnd(value) }
    }

    fun updateServiceEnabled(value: Boolean) {
        launchSafe("تعديل حالة تشغيل الخدمة", "updating master service toggle") { settingsManager.updateServiceEnabled(value) }
    }

    fun updateReplyMode(value: String) {
        launchSafe("تعديل نظام توليد الردود", "updating reply mode") { settingsManager.updateReplyMode(value) }
    }

    fun updateApiUrl(value: String) {
        launchSafe("تحديث رابط واجهة البيانات API", "updating custom Web API URL") { settingsManager.updateApiUrl(value) }
    }

    fun updateApiMethod(value: String) {
        launchSafe("تعديل طريقة طلب رابط الـ API", "updating custom Web API method") { settingsManager.updateApiMethod(value) }
    }

    fun updateApiHeaders(value: String) {
        launchSafe("تحديث عناوين الرأس للطلب", "updating custom Web API headers") { settingsManager.updateApiHeaders(value) }
    }

    fun updateApiBodyTemplate(value: String) {
        launchSafe("تحديث قالب نص الطلب", "updating custom Web API payload body template") { settingsManager.updateApiBodyTemplate(value) }
    }

    fun updateApiResponsePath(value: String) {
        launchSafe("تحديث مسار استخراج قيمة الرد", "updating custom Web API response path extraction") { settingsManager.updateApiResponsePath(value) }
    }

    fun updateAppLanguage(value: String) {
        launchSafe("تعديل لغة التطبيق الحالية", "updating current system language") { settingsManager.updateAppLanguage(value) }
    }

    fun updateCallReplyEnabled(value: Boolean) {
        launchSafe("تعديل تفعيل الرد التلقائي للمكالمات", "updating call auto-responder toggle") { settingsManager.updateCallReplyEnabled(value) }
    }

    fun updateCallReplyText(value: String) {
        launchSafe("تعديل رسالة الرد المحددة للمكالمات", "updating call auto-responder message") { settingsManager.updateCallReplyText(value) }
    }

    fun updateRingerVolume(value: Int) {
        launchSafe("تعديل مستوى صوت نغمة الاتصال", "updating ringer audio volume level") { settingsManager.updateRingerVolume(value) }
    }

    fun updateMediaVolume(value: Int) {
        launchSafe("تعديل مستوى صوت الوسائط", "updating media audio volume level") { settingsManager.updateMediaVolume(value) }
    }

    fun updateRingerMode(value: Int) {
        launchSafe("تعديل النمط الحالي للصوت", "updating device incoming call sound mode") { settingsManager.updateRingerMode(value) }
    }

    fun updateDismissNotificationsEnabled(value: Boolean) {
        launchSafe("تحديث كتم وإخفاء إشعارات الشاشة", "updating screen notifications dismissal setting") { settingsManager.updateDismissNotificationsEnabled(value) }
    }

    fun updateVoiceReplyEnabled(value: Boolean) {
        launchSafe("تحديث النطق والرد الصوتي التلقائي", "updating voice synthesis / text-to-speech announcement switch") { settingsManager.updateVoiceReplyEnabled(value) }
    }

    // --- Custom API Connection Testing ---
    fun testApiConnection() {
        viewModelScope.launch {
            try {
                _isTestingConnection.value = true
                _testConnectionResult.value = if (settings.value.appLanguage == "en") "Connecting to Custom API..." else "جاري الاتصال والتحقق من واجهة البيانات..."
                val result = customApiRepository.generateReply(
                    apiUrl = settings.value.apiUrl,
                    apiMethod = settings.value.apiMethod,
                    apiHeaders = settings.value.apiHeaders,
                    apiBodyTemplate = settings.value.apiBodyTemplate,
                    apiResponsePath = settings.value.apiResponsePath,
                    sender = "Fahd (Test)",
                    message = "Testing connection."
                )
                _isTestingConnection.value = false
                result.onSuccess { text ->
                    _testConnectionResult.value = "Success: $text"
                }.onFailure { e ->
                    _testConnectionResult.value = "Failed: ${e.message}"
                }
            } catch (ex: Exception) {
                _isTestingConnection.value = false
                _testConnectionResult.value = "Connection Test Crash: ${ex.message}"
                launchSafe("فحص اتصال واجهة البيانات", "custom Web API connection test connection crash") {
                    throw ex
                }
            }
        }
    }

    fun resetConnectionTestResult() {
        _testConnectionResult.value = null
    }

    fun updatePrimaryAccountPhone(value: String) {
        launchSafe("تحديث الحساب الأساسي", "updating primary account phone") {
            settingsManager.updatePrimaryAccountPhone(value)
        }
    }

    fun updateAdditionalAccountPhones(value: String) {
        launchSafe("تحديث الحسابات المتعددة الإضافية", "updating additional multi-account phones list") {
            settingsManager.updateAdditionalAccountPhones(value)
        }
    }

    fun updateInteractiveVoiceCallEnabled(value: Boolean) {
        launchSafe("تحديث المجيب التفاعلي للمكالمات", "updating interactive voice call enabled") {
            settingsManager.updateInteractiveVoiceCallEnabled(value)
        }
    }

    fun updateInteractiveVoiceCallPrompt(value: String) {
        launchSafe("تحديث موجه المجيب التفاعلي", "updating interactive voice call prompt") {
            settingsManager.updateInteractiveVoiceCallPrompt(value)
        }
    }
}
