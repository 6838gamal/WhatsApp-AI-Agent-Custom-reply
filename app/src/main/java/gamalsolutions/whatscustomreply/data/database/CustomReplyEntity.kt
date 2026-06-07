package gamalsolutions.whatscustomreply.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_replies")
data class CustomReplyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val keyword: String,
    val replyText: String,
    val isEnabled: Boolean = true,
    val contactName: String? = null
)
