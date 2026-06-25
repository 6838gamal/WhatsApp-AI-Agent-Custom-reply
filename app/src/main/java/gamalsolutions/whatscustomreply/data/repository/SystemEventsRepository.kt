package gamalsolutions.whatscustomreply.data.repository

import gamalsolutions.whatscustomreply.data.database.SystemEventDao
import gamalsolutions.whatscustomreply.data.database.SystemEventEntity
import kotlinx.coroutines.flow.Flow

class SystemEventsRepository(private val systemEventDao: SystemEventDao) {

    val allEvents: Flow<List<SystemEventEntity>> = systemEventDao.getAllEvents()
    val eventCount: Flow<Int> = systemEventDao.getEventCount()

    suspend fun insertEvent(event: SystemEventEntity) {
        systemEventDao.insertEvent(event)
    }

    suspend fun clearAllEvents() {
        systemEventDao.clearAllEvents()
    }

    suspend fun recordEvent(
        eventType: String,
        eventCategory: String,
        entityType: String,
        entityId: String,
        customerId: String,
        conversationId: String,
        message: String,
        metadata: String = ""
    ) {
        val event = SystemEventEntity(
            eventType = eventType,
            eventCategory = eventCategory,
            entityType = entityType,
            entityId = entityId,
            customerId = customerId,
            conversationId = conversationId,
            message = message,
            metadata = metadata
        )
        systemEventDao.insertEvent(event)
    }
}
