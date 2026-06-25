package gamalsolutions.whatscustomreply.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomReplyDao {
    @Query("SELECT * FROM custom_replies ORDER BY id DESC")
    fun getAllReplies(): Flow<List<CustomReplyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: CustomReplyEntity)

    @Update
    suspend fun updateReply(reply: CustomReplyEntity)

    @Delete
    suspend fun deleteReply(reply: CustomReplyEntity)

    @Query("SELECT * FROM custom_replies WHERE isEnabled = 1")
    suspend fun getEnabledReplies(): List<CustomReplyEntity>
}

@Dao
interface AutoReplyLogDao {
    @Query("SELECT * FROM reply_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AutoReplyLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AutoReplyLogEntity)

    @Query("DELETE FROM reply_logs")
    suspend fun clearAllLogs()

    @Query("SELECT COUNT(*) FROM reply_logs")
    fun getLogCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM reply_logs WHERE isSuccess = 1")
    fun getSuccessCount(): Flow<Int>
}

@Dao
interface SystemEventDao {
    @Query("SELECT * FROM system_events ORDER BY createdAt DESC")
    fun getAllEvents(): Flow<List<SystemEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: SystemEventEntity)

    @Query("DELETE FROM system_events")
    suspend fun clearAllEvents()

    @Query("SELECT COUNT(*) FROM system_events")
    fun getEventCount(): Flow<Int>
}
