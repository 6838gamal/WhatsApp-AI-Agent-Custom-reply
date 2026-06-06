package gamalsolutions.whatscustomreply.data.repository

import gamalsolutions.whatscustomreply.data.database.AutoReplyLogDao
import gamalsolutions.whatscustomreply.data.database.AutoReplyLogEntity
import kotlinx.coroutines.flow.Flow

class LogsRepository(private val autoReplyLogDao: AutoReplyLogDao) {
    val allLogs: Flow<List<AutoReplyLogEntity>> = autoReplyLogDao.getAllLogs()
    val logCount: Flow<Int> = autoReplyLogDao.getLogCount()
    val successCount: Flow<Int> = autoReplyLogDao.getSuccessCount()

    suspend fun insertLog(log: AutoReplyLogEntity) = autoReplyLogDao.insertLog(log)

    suspend fun clearAllLogs() = autoReplyLogDao.clearAllLogs()
}
