package gamalsolutions.whatscustomreply.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_replies")
data class CustomReplyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val replyText: String,
    val isEnabled: Boolean = true,
    val contactName: String? = null,
    val triggerType: String = "CHAT", // "CHAT", "CALL_ACTIVE", "CALL_MISSED"
    val replyType: String = "TEXT",   // "TEXT", "VOICE"
    val targetAccount: String? = null // Target phone/account filter, or null for all
)
