package gamalsolutions.whatscustomreply.data.repository

import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import java.util.Locale

object BusinessIntelligenceEngine {

    enum class LeadStatus { HOT, WARM, COLD }
    enum class OpportunityType { MISSED_SALE, HIGH_DEMAND, UNAVAILABLE_SERVICE, EXPANSION }
    enum class RiskType { CHURN, UNHAPPY, IDLE }
    enum class FollowUpType { PRICE, SERVICE, QUOTE, UNANSWERED, SILENT }
    enum class Intent { PURCHASE, PRICE_INQUIRY, SUPPORT, COMPLAINT, FOLLOW_UP, BOOKING, GENERAL_QUESTION, SERVICE_REQUEST }

    data class LeadAnalysis(
        val senderName: String,
        val status: LeadStatus,
        val reason: String,
        val score: Int,
        val tags: List<String>,
        val lastMessage: String,
        val timestamp: Long
    )

    data class OpportunityAnalysis(
        val type: OpportunityType,
        val title: String,
        val description: String,
        val sender: String,
        val timestamp: Long
    )

    data class RiskAnalysis(
        val senderName: String,
        val type: RiskType,
        val message: String,
        val severity: String, // "HIGH", "MEDIUM", "LOW"
        val timestamp: Long
    )

    data class FollowUpItem(
        val senderName: String,
        val type: FollowUpType,
        val lastMessage: String,
        val timestamp: Long,
        val isPending: Boolean
    )

    data class KnowledgeGapItem(
        val question: String,
        val frequency: Int,
        val category: String, // "CONFUSING" or "MISSING_INFO"
        val timestamp: Long
    )

    data class BIDashboardData(
        val leads: List<LeadAnalysis> = emptyList(),
        val opportunities: List<OpportunityAnalysis> = emptyList(),
        val risks: List<RiskAnalysis> = emptyList(),
        val followUps: List<FollowUpItem> = emptyList(),
        val knowledgeGaps: List<KnowledgeGapItem> = emptyList(),
        val intentDistribution: Map<Intent, Int> = emptyMap(),
        val hotLeadsCount: Int = 0,
        val warmLeadsCount: Int = 0,
        val coldLeadsCount: Int = 0,
        val activeCustomersCount: Int = 0,
        val idleCustomersCount: Int = 0,
        val topProductsDemanded: List<Pair<String, Int>> = emptyList(),
        val topServicesDemanded: List<Pair<String, Int>> = emptyList()
    )

    // Analyze full database logs
    fun analyzeLogs(logs: List<AutoReplyLogEntity>): BIDashboardData {
        if (logs.isEmpty()) return BIDashboardData()

        val logsByUser = logs.groupBy { it.senderName }
        val leads = mutableListOf<LeadAnalysis>()
        val opportunities = mutableListOf<OpportunityAnalysis>()
        val risks = mutableListOf<RiskAnalysis>()
        val followUps = mutableListOf<FollowUpItem>()
        val knowledgeGaps = mutableListOf<KnowledgeGapItem>()
        val intentsCount = mutableMapOf<Intent, Int>()

        val productDemands = mutableMapOf<String, Int>()
        val serviceDemands = mutableMapOf<String, Int>()
        val questionCounts = mutableMapOf<String, Int>()

        val now = System.currentTimeMillis()

        for ((user, userLogs) in logsByUser) {
            val sortedLogs = userLogs.sortedBy { it.timestamp }
            val lastLog = sortedLogs.last()
            val lastMsg = lastLog.messageText.lowercase(Locale.ROOT).trim()
            val lastReply = lastLog.replyText.lowercase(Locale.ROOT).trim()

            // 1. Intent Detection
            val userIntents = userLogs.map { detectIntent(it.messageText) }
            userIntents.forEach { intent ->
                intentsCount[intent] = (intentsCount[intent] ?: 0) + 1
            }
            val primaryIntent = detectIntent(lastLog.messageText)

            // 2. Lead Intelligence & Score
            var score = 0
            val leadTags = mutableListOf<String>()
            val reasons = mutableListOf<String>()

            // Frequency
            if (userLogs.size >= 5) {
                score += 30
                leadTags.add("VIP")
                leadTags.add("Frequent Buyer")
                reasons.add("عميل نشط جداً وتواصل متكرر")
            } else if (userLogs.size >= 2) {
                score += 15
                leadTags.add("Returning Customer")
                reasons.add("عميل عائد تواصل أكثر من مرة")
            }

            // Price Sensitivity
            val priceKeywords = listOf("سعر", "بكم", "خصم", "رخيص", "تخفيض", "سعركم", "قيمة", "price", "how much", "discount", "cheap")
            val priceInquiriesCount = userLogs.count { log -> priceKeywords.any { log.messageText.contains(it, ignoreCase = true) } }
            if (priceInquiriesCount > 0) {
                leadTags.add("Price Sensitive")
                score += 10
            }

            // Keyword triggers for Lead status
            val hotKeywords = listOf("شراء", "اشتراك", "حجز", "اريد شراء", "طلب منتج", "buy", "order", "subscribe", "book", "فاتورة", "بوابات دفع", "أريد شراء", "أبي أطلب")
            val warmKeywords = listOf("كيف", "تفاصيل", "ممكن معلومات", "details", "info", "how to", "استفسار", "متوفر", "هل عندكم", "وين موقعكم", "وين", "اين")
            val coldKeywords = listOf("شكراً", "تم", "تسلم", "ok", "thanks", "hello", "مرحبا", "السلام عليكم", "هلا", "hi")

            val containsHot = userLogs.any { log -> hotKeywords.any { log.messageText.contains(it, ignoreCase = true) } }
            val containsWarm = userLogs.any { log -> warmKeywords.any { log.messageText.contains(it, ignoreCase = true) } }

            val status = when {
                containsHot -> {
                    score += 50
                    leadTags.add("Hot Lead")
                    leadTags.add("High Value")
                    reasons.add("أبدى اهتماماً صريحاً بالشراء أو الحجز")
                    LeadStatus.HOT
                }
                containsWarm -> {
                    score += 25
                    leadTags.add("Warm Lead")
                    leadTags.add("Interested")
                    reasons.add("يستفسر عن التفاصيل أو يسأل عن توفر الخدمات")
                    LeadStatus.WARM
                }
                else -> {
                    leadTags.add("Cold Lead")
                    reasons.add("تواصل عام أو تحية دون إبداء اهتمام تجاري صريح")
                    LeadStatus.COLD
                }
            }

            leads.add(
                LeadAnalysis(
                    senderName = user,
                    status = status,
                    reason = reasons.joinToString(" و "),
                    score = score.coerceIn(0, 100),
                    tags = leadTags,
                    lastMessage = lastLog.messageText,
                    timestamp = lastLog.timestamp
                )
            )

            // 3. Opportunity Detection
            // Missed Sale: hot intent or price inquiry and failed reply or unhelpful default reply
            val isMissedSale = (primaryIntent == Intent.PURCHASE || primaryIntent == Intent.PRICE_INQUIRY) && !lastLog.isSuccess
            if (isMissedSale) {
                opportunities.add(
                    OpportunityAnalysis(
                        type = OpportunityType.MISSED_SALE,
                        title = "فرصة بيع ضائعة - عدم الرد",
                        description = "العميل استفسر عن السعر أو أبدى نية الشراء ولكن فشل النظام في الرد تلقائياً.",
                        sender = user,
                        timestamp = lastLog.timestamp
                    )
                )
            }

            // Extract Demanded Services & Products
            extractDemandedItems(lastLog.messageText, productDemands, serviceDemands)

            // Services Not Available
            val unavailableKeywords = listOf("غير متوفر", "لا يوجد", "ما عندكم", "هل توفرون", "هل عندكم توصيل", "عندكم صيانة", "not available", "do you provide", "delivery")
            if (unavailableKeywords.any { lastLog.messageText.contains(it, ignoreCase = true) }) {
                opportunities.add(
                    OpportunityAnalysis(
                        type = OpportunityType.UNAVAILABLE_SERVICE,
                        title = "طلب خدمة غير متوفرة حالياً",
                        description = "العميل يسأل عن خدمة أو ميزة قد لا تكون متوفرة في ردودك التلقائية: \"${lastLog.messageText}\"",
                        sender = user,
                        timestamp = lastLog.timestamp
                    )
                )
            }

            // 4. Customer Risk Detection
            val churnKeywords = listOf("إلغاء", "حذف", "استرجاع", "cancel", "refund", "إلغاء الاشتراك", "توقف", "تعطيل")
            val complaintKeywords = listOf("سيء", "مشكلة", "لا يعمل", "عطل", "بطيء", "worst", "broken", "issue", "complaint", "شكوى", "تأخرتم", "سرقة", "كذب", "خطأ", "غلط")

            val isChurnRisk = userLogs.any { log -> churnKeywords.any { log.messageText.contains(it, ignoreCase = true) } }
            val isComplaint = userLogs.any { log -> complaintKeywords.any { log.messageText.contains(it, ignoreCase = true) } }

            val daysIdle = (now - lastLog.timestamp) / (1000 * 60 * 60 * 24)
            val isIdle = daysIdle >= 3

            if (isChurnRisk) {
                risks.add(
                    RiskAnalysis(
                        senderName = user,
                        type = RiskType.CHURN,
                        message = "طلب إلغاء أو استرجاع أموال: \"${lastLog.messageText}\"",
                        severity = "HIGH",
                        timestamp = lastLog.timestamp
                    )
                )
                leadTags.add("Complaint Risk")
            } else if (isComplaint) {
                risks.add(
                    RiskAnalysis(
                        senderName = user,
                        type = RiskType.UNHAPPY,
                        message = "العميل يشتكي من الخدمة أو واجه مشكلة: \"${lastLog.messageText}\"",
                        severity = "MEDIUM",
                        timestamp = lastLog.timestamp
                    )
                )
                leadTags.add("Complaint Risk")
            } else if (isIdle && userLogs.size >= 3) {
                risks.add(
                    RiskAnalysis(
                        senderName = user,
                        type = RiskType.IDLE,
                        message = "عميل متكرر توقف عن التفاعل منذ $daysIdle أيام.",
                        severity = "LOW",
                        timestamp = lastLog.timestamp
                    )
                )
            }

            // 5. Follow-Up Engine
            // Needs Follow Up: asked for price, quote, service and last log was more than 1 hour ago
            val needsFollowUpKeywords = listOf("بكم", "سعر", "عرض", "خدمة", "أبي", "اريد", "price", "quote", "service")
            val lastLogIsUserRequest = lastLog.replyText.contains("No matching reply") || !lastLog.isSuccess
            val hasFollowUpIntent = primaryIntent == Intent.PRICE_INQUIRY || primaryIntent == Intent.SERVICE_REQUEST || primaryIntent == Intent.BOOKING
            
            if (hasFollowUpIntent || (lastLogIsUserRequest && needsFollowUpKeywords.any { lastLog.messageText.contains(it, ignoreCase = true) })) {
                followUps.add(
                    FollowUpItem(
                        senderName = user,
                        type = if (primaryIntent == Intent.PRICE_INQUIRY) FollowUpType.PRICE else FollowUpType.SERVICE,
                        lastMessage = lastLog.messageText,
                        timestamp = lastLog.timestamp,
                        isPending = true
                    )
                )
                leadTags.add("Needs Follow Up")
            } else if (isIdle) {
                followUps.add(
                    FollowUpItem(
                        senderName = user,
                        type = FollowUpType.SILENT,
                        lastMessage = lastLog.messageText,
                        timestamp = lastLog.timestamp,
                        isPending = true
                    )
                )
            }

            // 6. Knowledge Gap Detection
            // Questions containing key elements or ending with "?" or "؟"
            userLogs.forEach { log ->
                val txt = log.messageText.trim()
                if (txt.endsWith("؟") || txt.endsWith("?") || txt.startsWith("كيف") || txt.startsWith("لماذا") || txt.startsWith("وين") || txt.startsWith("ما هي")) {
                    val cleanQuestion = txt.replace("?", "").replace("؟", "").trim()
                    if (cleanQuestion.length > 8) {
                        questionCounts[cleanQuestion] = (questionCounts[cleanQuestion] ?: 0) + 1
                    }
                }
            }
        }

        // Aggregate knowledge gaps
        questionCounts.forEach { (q, count) ->
            val isConfusing = count >= 2
            knowledgeGaps.add(
                KnowledgeGapItem(
                    question = q,
                    frequency = count,
                    category = if (isConfusing) "CONFUSING" else "MISSING_INFO",
                    timestamp = now
                )
            )
        }

        // Metrics calculations
        val hotCount = leads.count { it.status == LeadStatus.HOT }
        val warmCount = leads.count { it.status == LeadStatus.WARM }
        val coldCount = leads.count { it.status == LeadStatus.COLD }

        val activeCount = logsByUser.filter { (_, uLogs) ->
            val lastTime = uLogs.maxOf { it.timestamp }
            (now - lastTime) < (24 * 60 * 60 * 1000L) // active within last 24h
        }.size

        val idleCount = logsByUser.size - activeCount

        return BIDashboardData(
            leads = leads.sortedByDescending { it.score },
            opportunities = opportunities.sortedByDescending { it.timestamp },
            risks = risks.sortedBy { if (it.severity == "HIGH") 1 else if (it.severity == "MEDIUM") 2 else 3 },
            followUps = followUps.sortedByDescending { it.timestamp },
            knowledgeGaps = knowledgeGaps.sortedByDescending { it.frequency },
            intentDistribution = intentsCount,
            hotLeadsCount = hotCount,
            warmLeadsCount = warmCount,
            coldLeadsCount = coldCount,
            activeCustomersCount = activeCount,
            idleCustomersCount = idleCount,
            topProductsDemanded = productDemands.toList().sortedByDescending { it.second }.take(5),
            topServicesDemanded = serviceDemands.toList().sortedByDescending { it.second }.take(5)
        )
    }

    // Identify Intent
    fun detectIntent(message: String): Intent {
        val msg = message.lowercase(Locale.ROOT)
        return when {
            msg.contains("شراء") || msg.contains("اشتري") || msg.contains("buy") || msg.contains("اطلب") || msg.contains("طلب منتج") -> Intent.PURCHASE
            msg.contains("سعر") || msg.contains("بكم") || msg.contains("خصم") || msg.contains("كم السعر") || msg.contains("قيمته") || msg.contains("price") || msg.contains("cost") || msg.contains("how much") -> Intent.PRICE_INQUIRY
            msg.contains("حجز") || msg.contains("موعد") || msg.contains("احجز") || msg.contains("book") || msg.contains("appointment") -> Intent.BOOKING
            msg.contains("مشكلة") || msg.contains("سيء") || msg.contains("عطل") || msg.contains("شكوى") || msg.contains("خراب") || msg.contains("broken") || msg.contains("error") -> Intent.COMPLAINT
            msg.contains("متابعة") || msg.contains("وصل") || msg.contains("وين الطلب") || msg.contains("تاخر") || msg.contains("follow up") -> Intent.FOLLOW_UP
            msg.contains("دعم") || msg.contains("مساعدة") || msg.contains("صيانة") || msg.contains("support") || msg.contains("help") -> Intent.SUPPORT
            msg.contains("خدمة") || msg.contains("توصيل") || msg.contains("شحن") || msg.contains("service") -> Intent.SERVICE_REQUEST
            else -> Intent.GENERAL_QUESTION
        }
    }

    // Helper to extract requested items
    private fun extractDemandedItems(message: String, products: MutableMap<String, Int>, services: MutableMap<String, Int>) {
        val msg = message.lowercase(Locale.ROOT)
        
        // Products regex-like extraction
        val productWords = listOf("منتج", "كتاب", "عطر", "جهاز", "هاتف", "فلتر", "حقيبة", "ساعة", "شاحن", "ملابس", "product", "item", "phone", "device")
        val serviceWords = listOf("خدمة", "توصيل", "برمجة", "تصميم", "صيانة", "تركيب", "تنظيف", "شحن", "استشارة", "تدريب", "دورة", "service", "delivery", "design", "hosting")

        productWords.forEach { word ->
            if (msg.contains(word)) {
                val index = msg.indexOf(word)
                val substring = msg.substring(index).split(" ").take(3).joinToString(" ")
                products[substring] = (products[substring] ?: 0) + 1
            }
        }

        serviceWords.forEach { word ->
            if (msg.contains(word)) {
                val index = msg.indexOf(word)
                val substring = msg.substring(index).split(" ").take(3).joinToString(" ")
                services[substring] = (services[substring] ?: 0) + 1
            }
        }
    }
}
