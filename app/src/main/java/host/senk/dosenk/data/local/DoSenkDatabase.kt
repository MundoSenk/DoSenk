package host.senk.dosenk.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.entity.ScheduleEntity
import host.senk.dosenk.data.local.entity.UserEntity

@Database(
    entities = [UserEntity::class, ScheduleEntity::class], // Tus tablas
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // traductor de JSON
abstract class DoSenkDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao // La puerta para usar el DAO
}