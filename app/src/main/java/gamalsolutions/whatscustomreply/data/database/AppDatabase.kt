package gamalsolutions.whatscustomreply.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CustomReplyEntity::class, AutoReplyLogEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun customReplyDao(): CustomReplyDao
    abstract fun autoReplyLogDao(): AutoReplyLogDao
}
