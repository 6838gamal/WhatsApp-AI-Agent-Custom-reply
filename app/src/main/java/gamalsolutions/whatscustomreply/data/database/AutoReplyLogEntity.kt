package gamalsolutions.whatscustomreply.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reply_logs")
data class AutoReplyLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderName: String,
    val messageText: String,
    val replyText: String,
    val mode: String, // "CUSTOM", "GEMINI", "HYBRID", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean
)
