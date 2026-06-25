package gamalsolutions.whatscustomreply.data.repository

import gamalsolutions.whatscustomreply.data.database.AutoReplyLogDao
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import kotlinx.coroutines.flow.Flow

class LogsRepository(
    private val autoReplyLogDao: AutoReplyLogDao,
    private val systemEventsRepository: SystemEventsRepository
) {
    val allLogs: Flow<List<AutoReplyLogEntity>> = autoReplyLogDao.getAllLogs()
    val logCount: Flow<Int> = autoReplyLogDao.getLogCount()
    val successCount: Flow<Int> = autoReplyLogDao.getSuccessCount()

    suspend fun insertLog(log: AutoReplyLogEntity) {
        autoReplyLogDao.insertLog(log)
        
        // Dynamic Intelligence classification upon log entry
        val intent = BusinessIntelligenceEngine.detectIntent(log.messageText)
        val category = if (log.isSuccess) "OUTGOING" else "INCOMING"
        val metadata = "{\"intent\":\"$intent\",\"isSuccess\":${log.isSuccess},\"mode\":\"${log.mode}\"}"
        
        systemEventsRepository.recordEvent(
            eventType = "MESSAGE",
            eventCategory = category,
            entityType = "MESSAGE",
            entityId = log.id.toString(),
            customerId = log.senderName,
            conversationId = log.senderName,
            message = "Msg: ${log.messageText} | Reply: ${log.replyText}",
            metadata = metadata
        )

        // If a message was failed/unanswered, record a follow-up or opportunity event
        if (!log.isSuccess || log.mode.contains("FAILED") || log.replyText.contains("No matching reply")) {
            systemEventsRepository.recordEvent(
                eventType = "OPPORTUNITY",
                eventCategory = "SYSTEM",
                entityType = "MESSAGE",
                entityId = log.id.toString(),
                customerId = log.senderName,
                conversationId = log.senderName,
                message = "فرصة تواصل جديدة مع العميل ${log.senderName} لم يتم تلبيتها تلقائياً",
                metadata = "{\"intent\":\"$intent\",\"opportunityType\":\"MISSED_SALE\"}"
            )
        }
    }

    suspend fun clearAllLogs() {
        autoReplyLogDao.clearAllLogs()
    }
}
