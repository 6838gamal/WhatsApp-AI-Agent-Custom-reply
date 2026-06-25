package gamalsolutions.whatscustomreply.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CustomReplyEntity::class, AutoReplyLogEntity::class, SystemEventEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customReplyDao(): CustomReplyDao
    abstract fun autoReplyLogDao(): AutoReplyLogDao
    abstract fun systemEventDao(): SystemEventDao
}
