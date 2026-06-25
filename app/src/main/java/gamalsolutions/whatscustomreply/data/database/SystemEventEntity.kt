package gamalsolutions.whatscustomreply.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_events")
data class SystemEventEntity(
    @PrimaryKey(autoGenerate = true) val eventId: Int = 0,
    val eventType: String,       // MESSAGE, ANALYSIS, ERROR, UPDATE, FOLLOW_UP, OPPORTUNITY, AUDIT
    val eventCategory: String,   // INCOMING, OUTGOING, SYSTEM, SECURITY
    val entityType: String,      // MESSAGE, CUSTOMER, RULE, SETTING
    val entityId: String,        // ID or Name of entity
    val customerId: String,      // Sender/Contact name or phone
    val conversationId: String,  // Conversation ID (usually sender name)
    val message: String,         // Event message
    val metadata: String,        // JSON or key-value details
    val createdAt: Long = System.currentTimeMillis()
)
