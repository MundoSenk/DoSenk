package host.senk.dosenk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.dao.MissionDao
import host.senk.dosenk.data.local.entity.ScheduleEntity
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.local.entity.MissionEntity

@Database(
    entities = [UserEntity::class, ScheduleEntity::class, MissionEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DoSenkDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun missionDao(): MissionDao
}