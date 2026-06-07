package gamalsolutions.whatscustomreply.ui

sealed class LocStrings {
    abstract val appName: String
    abstract val home: String
    abstract val rules: String
    abstract val gemini: String
    abstract val logs: String
    abstract val stats: String
    abstract val settings: String
    
    // DashboardScreen
    abstract val dashboardTitle: String
    abstract val dashboardSubtitle: String
    abstract val notificationAccessHeader: String
    abstract val notificationAccessBody: String
    abstract val grantPermission: String
    abstract val globalStatusOn: String
    abstract val globalStatusOff: String
    abstract val activeMode: String
    abstract val repliesModeTitle: String
    abstract val repliesModeDesc: String
    abstract val metricsTotalReplies: String
    abstract val metricsSuccessfulReplies: String
    abstract val metricsSuccessRate: String
    abstract val quickShortcuts: String
    abstract val rulesBuilder: String
    abstract val rulesBuilderDesc: String
    abstract val geminiSettings: String
    abstract val geminiSettingsDesc: String
    abstract val filterPreferences: String
    abstract val filterPreferencesDesc: String
    abstract val simulationHeader: String
    abstract val simulationBody: String
    abstract val simSenderLabel: String
    abstract val simMessageLabel: String
    abstract val simulateBtn: String
    abstract val simResultsHeader: String
    abstract val simSuccessMsg: String
    abstract val simFailureMsg: String
    
    // RepliesScreen
    abstract val customRepliesHeader: String
    abstract val customRepliesDesc: String
    abstract val noRepliesConfigured: String
    abstract val noRepliesRecommendation: String
    abstract val createFirstRule: String
    abstract val matchKeywordLabel: String
    abstract val matchKeywordPlaceholder: String
    abstract val replyTextLabel: String
    abstract val replyTextPlaceholder: String
    abstract val addRuleTitle: String
    abstract val addRuleSubtitle: String
    abstract val editRuleTitle: String
    abstract val saveRuleBtn: String
    abstract val deleteRuleBtn: String
    abstract val cancelBtn: String
    abstract val saveBtn: String
    abstract val fieldsRequiredError: String
    abstract val containsTag: String
    
    // Contact specific rule string
    abstract val contactNameLabel: String
    abstract val contactNamePlaceholder: String
    abstract val contactMatchHeader: String
    abstract val contactMatchBody: String
    abstract val contactSpecificOnly: String
    abstract val appliesToAll: String
    
    // Additional contact-specific strings
    abstract val searchPlaceholder: String
    abstract val quickAddRuleForContact: String
    abstract val filterAll: String
    abstract val filterGlobal: String
    abstract val noRepliesForSelectedContact: String
    
    // GeminiSettingsScreen
    abstract val geminiEngineHeader: String
    abstract val geminiEngineDesc: String
    abstract val apiKeyLabel: String
    abstract val testConnectionBtn: String
    abstract val testingConnection: String
    abstract val selectModel: String
    abstract val customSystemPrompt: String
    abstract val customSystemPromptDesc: String
    
    // LogsScreen
    abstract val logHeader: String
    abstract val logDesc: String
    abstract val noLogs: String
    abstract val clearHistory: String
    
    // SettingsScreen
    abstract val generalPreferences: String
    abstract val generalPreferencesDesc: String
    abstract val ignoreGroupsSetting: String
    abstract val ignoreGroupsSettingDesc: String
    abstract val ignoreDuplicatesSetting: String
    abstract val ignoreDuplicatesSettingDesc: String
    abstract val replyOnceSetting: String
    abstract val replyOnceSettingDesc: String
    abstract val randomDelaySetting: String
    abstract val randomDelaySettingDesc: String
    abstract val workingHoursSetting: String
    abstract val workingHoursSettingDesc: String
    abstract val workingHoursStartLabel: String
    abstract val workingHoursEndLabel: String
    abstract val changeLanguageSetting: String
    abstract val changeLanguageSettingDesc: String
    abstract val appLanguageLabel: String
    
    // Modes
    abstract val modeCustom: String
    abstract val modeCustomDesc: String
    abstract val modeGemini: String
    abstract val modeGeminiDesc: String
    abstract val modeHybrid: String
    abstract val modeHybridDesc: String
}

object ArStrings : LocStrings() {
    override val appName = "المجيب الآلي المخصص"
    override val home = "الرئيسية"
    override val rules = "القواعد"
    override val gemini = "جيميني"
    override val logs = "السجلات"
    override val stats = "الإحصائيات"
    override val settings = "الإعدادات"
    
    override val dashboardTitle = "المجيب الذكي للواتساب عبر الذكاء الاصطناعي"
    override val dashboardSubtitle = "مساعد دردشة تلقائي يعمل محليًا بالكامل داخل جهازك."
    override val notificationAccessHeader = "مطلوب صلاحية الوصول للإشعارات"
    override val notificationAccessBody = "يتطلب أندرويد تفعيل صلاحية الوصول للإشعارات لهذا التطبيق ليتمكن من قراءة إشعارات واتساب الواردة والرد عليها تلقائيًا."
    override val grantPermission = "منح الصلاحية"
    override val globalStatusOn = "المجيب التلقائي نشط وفعال"
    override val globalStatusOff = "المجيب التلقائي متوقف حاليًا"
    override val activeMode = "الوضع النشط"
    override val repliesModeTitle = "محرك الرد التلقائي"
    override val repliesModeDesc = "حدد الطريقة التي ترغب بها في تصنيف والرد على الرسائل الواردة."
    override val metricsTotalReplies = "إجمالي الردود"
    override val metricsSuccessfulReplies = "الردود الناجحة"
    override val metricsSuccessRate = "نسبة النجاح"
    override val quickShortcuts = "الوصول السريع"
    override val rulesBuilder = "قواعد الرد المخصصة"
    override val rulesBuilderDesc = "إدارة الكلمات المفتاحية والردود الثابتة."
    override val geminiSettings = "إعدادات محرك جيميني"
    override val geminiSettingsDesc = "تهيئة مفتاح API واختيار النموذج وصياغة التوجيه الذكي."
    override val filterPreferences = "خيارات التصفية والقيود"
    override val filterPreferencesDesc = "تحديد أوقات العمل، والمهلة الزمنية، وتجنب الرد المتكرر المزعج."
    override val simulationHeader = "محاكاة الرد التلقائي للواتساب (التجربة والتشخيص)"
    override val simulationBody = "اختبر قواعد الرد المخصصة أو محرك جيميني مباشرة دون تفعيل الخدمة أو إرسال رسائل حقيقية."
    override val simSenderLabel = "اسم جهة الاتصال (المرسل)"
    override val simMessageLabel = "نص الرسالة الواردة"
    override val simulateBtn = "بدء المحاكاة"
    override val simResultsHeader = "نتائج المحاكاة"
    override val simSuccessMsg = "نجاح الرد التلقائي"
    override val simFailureMsg = "فشل الرد التلقائي"
    
    override val customRepliesHeader = "الردود المخصصة والذكية"
    override val customRepliesDesc = "قم بتهيئة نصوص العبارات المفتاحية للرد التلقائي التفاعلي فورًا."
    override val noRepliesConfigured = "لا توجد قواعد مخصصة مضافة بعد"
    override val noRepliesRecommendation = "قم بإضافة كلمات مفتاحية مثل 'السعر'، 'العنوان'، 'مرحبا' ليتولى التطبيق الرد الآلي على العملاء مباشرة وبسرعة فائقة."
    override val createFirstRule = "إضافة أول قاعدة رد"
    override val matchKeywordLabel = "الكلمة المفتاحية (تتضمن)"
    override val matchKeywordPlaceholder = "مثال: السعر"
    override val replyTextLabel = "نص الرد الآلي المخصص"
    override val replyTextPlaceholder = "مثال: أهلاً بك، أسعارنا تبدأ من ٢٠ دولارًا شهريًا."
    override val addRuleTitle = "إضافة قاعدة رد جديدة"
    override val addRuleSubtitle = "مطابقة أي رسالة واردة على واتساب تحتوي على هذه العبارة للرد عليها فورًا."
    override val editRuleTitle = "تعديل قاعدة الرد"
    override val saveRuleBtn = "إضافة القاعدة"
    override val deleteRuleBtn = "حذف"
    override val cancelBtn = "إلغاء"
    override val saveBtn = "حفظ"
    override val fieldsRequiredError = "يرجى ملء جميع الحقول المطلوبة للمتابعة."
    override val containsTag = "تتضمن"
    
    override val contactNameLabel = "جهة الاتصال المستهدفة (اختياري)"
    override val contactNamePlaceholder = "مثال: أحمد، أو اتركه فارغاً للجميع"
    override val contactMatchHeader = "قواعد مخصصة لجهات اتصال معينة"
    override val contactMatchBody = "حدد مستلمًا معينًا لتطبيق هذه القاعدة عليه فقط."
    override val contactSpecificOnly = "خاص بجهة الاتصال: "
    override val appliesToAll = "يطبق على كافة جهات الاتصال"
    
    // Additional contact-specific strings
    override val searchPlaceholder = "بحث بجهة اتصال أو كلمة..."
    override val quickAddRuleForContact = "إضافة رد مخصص لهذا المرسل"
    override val filterAll = "الكل"
    override val filterGlobal = "قواعد عامة"
    override val noRepliesForSelectedContact = "لا توجد ردود آليّة مخصصة لجهة الاتصال هذه بعد. يمكنك إضافة رد بالضغط على زر +"
    
    override val geminiEngineHeader = "إعدادات محرك الذكاء الاصطناعي (Gemini)"
    override val geminiEngineDesc = "تحكم في إعدادات الاتصال بمحرك جوجل جيميني للردود بالذكاء الاصطناعي."
    override val apiKeyLabel = "مفتاح API الخاص بجيميني"
    override val testConnectionBtn = "فحص الاتصال"
    override val testingConnection = "جاري الاتصال بجيميني وفحص المفتاح المضاف..."
    override val selectModel = "اختر إصدار نموذج جيميني"
    override val customSystemPrompt = "التوجيه الأساسي للنموذج (System Prompt)"
    override val customSystemPromptDesc = "وجه الذكاء الاصطناعي وحدد أسلوب تعامله وشخصيته وتصرفاته وطريقة رده بدقة."
    
    override val logHeader = "سجل الإرسال والردود"
    override val logDesc = "متابعة وتحليل الرسائل الواردة والردود الآلية المباشرة التي قام بها التطبيق."
    override val noLogs = "لا توجد سجلات رد وبث حاليًا."
    override val clearHistory = "مسح كافة السجلات"
    
    override val generalPreferences = "خيارات الخدمة والقيود"
    override val generalPreferencesDesc = "قم بضبط الحدود والشروط الخاصة بعملية الرد على الرسائل بدقة."
    override val ignoreGroupsSetting = "تجاهل الرسائل من المجموعات"
    override val ignoreGroupsSettingDesc = "تجنب الرد التلقائي داخل غرف الدردشة والمجموعات على واتساب."
    override val ignoreDuplicatesSetting = "تجاهل الرسائل المكررة"
    override val ignoreDuplicatesSettingDesc = "تجنب إرسال ردود مكررة لنفس محتوى الإشعارات المتتالية بسرعة."
    override val replyOnceSetting = "رد بمرة واحدة لكل جهة اتصال"
    override val replyOnceSettingDesc = "الرد مرة واحدة فقط على الشخص طوال مدة نشاط الخدمة."
    override val randomDelaySetting = "إضافة مهلة زمنية عشوائية قبل الرد"
    override val randomDelaySettingDesc = "محاكاة الرد البشري الطبيعي من خلال تعطيل الإرسال الفوري لعدة ثوانٍ."
    override val workingHoursSetting = "تفعيل أوقات العمل التشغيلية"
    override val workingHoursSettingDesc = "الرد الآلي فقط في الساعات المحددة، وتجاهل الرسائل خارجها."
    override val workingHoursStartLabel = "بداية وقت العمل"
    override val workingHoursEndLabel = "نهاية وقت العمل"
    override val changeLanguageSetting = "تغيير لغة عرض التطبيق"
    override val changeLanguageSettingDesc = "اختر اللغة المفضلة لواجهة المستخدم وكافة أنحاء التطبيق."
    override val appLanguageLabel = "لغة عرض التطبيق"
    
    override val modeCustom = "الردود المخصصة فقط"
    override val modeCustomDesc = "الاعتماد بالكامل على مطابقة الكلمات المفتاحية والقواعد اليدوية."
    override val modeGemini = "محرك جيميني الذكي"
    override val modeGeminiDesc = "توليد ردود تفاعلية طبيعية وذكية باستخدام Google Gemini."
    override val modeHybrid = "وضع الرد الهجين (المزدوج)"
    override val modeHybridDesc = "التحقق أولاً من الكلمات المفتاحية المخصصة، ثم استخدام جيميني كبديل ذكي."
}

object EnStrings : LocStrings() {
    override val appName = "WhatsCustomReply"
    override val home = "Home"
    override val rules = "Rules"
    override val gemini = "Gemini"
    override val logs = "Logs"
    override val stats = "Stats"
    override val settings = "Settings"
    
    override val dashboardTitle = "WhatsApp AI Auto-Responder"
    override val dashboardSubtitle = "Automated chat assistant running locally inside your device."
    override val notificationAccessHeader = "Notification Access Required"
    override val notificationAccessBody = "Android requires Notification Access permission for this app to detect incoming WhatsApp notifications and trigger replies."
    override val grantPermission = "Grant Permission"
    override val globalStatusOn = "Auto Reply is Active"
    override val globalStatusOff = "Auto Reply is Suspended"
    override val activeMode = "Active Mode"
    override val repliesModeTitle = "Auto Reply Mode Engine"
    override val repliesModeDesc = "Specify how incoming messages should be classified and answered."
    override val metricsTotalReplies = "Total Replies"
    override val metricsSuccessfulReplies = "Successful"
    override val metricsSuccessRate = "Success Rate"
    override val quickShortcuts = "Quick Shortcuts"
    override val rulesBuilder = "Custom Keyword Rules"
    override val rulesBuilderDesc = "Manage text matching keywords and static replies."
    override val geminiSettings = "Gemini AI Configuration"
    override val geminiSettingsDesc = "Configure API key, model version, and custom system context."
    override val filterPreferences = "Filters & Constraints"
    override val filterPreferencesDesc = "Define schedule boundaries, random delay, and prevent message spam."
    override val simulationHeader = "WhatsApp Reply Simulator (Testing & Diagnostics)"
    override val simulationBody = "Simulate custom keyword matching or Gemini generation directly without sending real notifications."
    override val simSenderLabel = "Contact Sender Name"
    override val simMessageLabel = "Incoming Message Body"
    override val simulateBtn = "Start Simulation"
    override val simResultsHeader = "Simulation Results"
    override val simSuccessMsg = "Successful Automated Reply"
    override val simFailureMsg = "Failed to Generate Reply"
    
    override val customRepliesHeader = "Custom Replies"
    override val customRepliesDesc = "Configure matching text terms to auto-respond with custom templates."
    override val noRepliesConfigured = "No custom replies configured"
    override val noRepliesRecommendation = "Add phrases like 'price', 'address', or 'help' so the auto-responder can handle conversations without your intervention."
    override val createFirstRule = "Create First Rule"
    override val matchKeywordLabel = "Keyword Phrase (Contains)"
    override val matchKeywordPlaceholder = "e.g. price"
    override val replyTextLabel = "Auto-Reply Text"
    override val replyTextPlaceholder = "e.g. Our basic plan tier is $20/month."
    override val addRuleTitle = "Add Custom Reply"
    override val addRuleSubtitle = "Match any incoming WhatsApp text containing this phrase to auto-respond instantly."
    override val editRuleTitle = "Edit Custom Reply"
    override val saveRuleBtn = "Save Rule"
    override val deleteRuleBtn = "Delete"
    override val cancelBtn = "Cancel"
    override val saveBtn = "Save"
    override val fieldsRequiredError = "Please enter both fields to save."
    override val containsTag = "Contains"
    
    override val contactNameLabel = "Target Contact Name (Optional)"
    override val contactNamePlaceholder = "e.g. John Doe, or leave empty for anyone"
    override val contactMatchHeader = "Target specific contacts"
    override val contactMatchBody = "Limit this reply rule to trigger only for a specific contact."
    override val contactSpecificOnly = "Specific Contact: "
    override val appliesToAll = "Applies to all contacts"
    
    // Additional contact-specific strings
    override val searchPlaceholder = "Search contact or keyword..."
    override val quickAddRuleForContact = "Add custom reply for contact"
    override val filterAll = "All"
    override val filterGlobal = "Global"
    override val noRepliesForSelectedContact = "No custom replies configured for this contact yet. Click '+' to add one."
    
    override val geminiEngineHeader = "Gemini AI Engine Configuration"
    override val geminiEngineDesc = "Modify settings for connecting to Google Gemini AI models."
    override val apiKeyLabel = "Gemini API Key"
    override val testConnectionBtn = "Test Connection"
    override val testingConnection = "Connecting to Gemini to verify connectivity..."
    override val selectModel = "Select Gemini Model"
    override val customSystemPrompt = "System Prompt"
    override val customSystemPromptDesc = "Instruct the AI helper how to adapt, behave, sound, and construct its replies."
    
    override val logHeader = "Replying Histograms"
    override val logDesc = "Follow and examine incoming messages, generated replies, and transmission logs."
    override val noLogs = "No reply logs are available currently."
    override val clearHistory = "Clear Logs History"
    
    override val generalPreferences = "Preferences & Constraints"
    override val generalPreferencesDesc = "Configure conditions and boundaries for automatic response replies."
    override val ignoreGroupsSetting = "Ignore Group Chats"
    override val ignoreGroupsSettingDesc = "Skip automated replies for WhatsApp group notifications."
    override val ignoreDuplicatesSetting = "Ignore Duplicate Messages"
    override val ignoreDuplicatesSettingDesc = "Prevent responding multiple times to redundant sequential alerts."
    override val replyOnceSetting = "Reply Once Per User"
    override val replyOnceSettingDesc = "Only send a single automated message per peer per session lifecycle."
    override val randomDelaySetting = "Add Random Reply Delay"
    override val randomDelaySettingDesc = "Synthesize human natural replies by holding back sending instantly for a few seconds."
    override val workingHoursSetting = "Enforce Operating Hours"
    override val workingHoursSettingDesc = "Restrict automated replies to trigger only during business hours."
    override val workingHoursStartLabel = "Working Hours Start"
    override val workingHoursEndLabel = "Working Hours End"
    override val changeLanguageSetting = "Change App Display Language"
    override val changeLanguageSettingDesc = "Change your default language preference for the entire app interface."
    override val appLanguageLabel = "App Display Language"
    
    override val modeCustom = "Custom Replies Only"
    override val modeCustomDesc = "Relies entirely on matching keywords."
    override val modeGemini = "Gemini AI Engine"
    override val modeGeminiDesc = "Generate smart, custom replies using Gemini Flash."
    override val modeHybrid = "Hybrid Responder Mode"
    override val modeHybridDesc = "Checks keywords first. Invokes Gemini as fallback."
}
