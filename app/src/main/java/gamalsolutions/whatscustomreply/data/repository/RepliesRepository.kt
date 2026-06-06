package gamalsolutions.whatscustomreply.data.repository

import gamalsolutions.whatscustomreply.data.database.CustomReplyDao
import gamalsolutions.whatscustomreply.data.database.CustomReplyEntity
import kotlinx.coroutines.flow.Flow

class RepliesRepository(private val customReplyDao: CustomReplyDao) {
    val allReplies: Flow<List<CustomReplyEntity>> = customReplyDao.getAllReplies()

    suspend fun insertReply(reply: CustomReplyEntity) = customReplyDao.insertReply(reply)

    suspend fun updateReply(reply: CustomReplyEntity) = customReplyDao.updateReply(reply)

    suspend fun deleteReply(reply: CustomReplyEntity) = customReplyDao.deleteReply(reply)

    suspend fun getEnabledReplies(): List<CustomReplyEntity> = customReplyDao.getEnabledReplies()
}
