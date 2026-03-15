package host.senk.dosenk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import host.senk.dosenk.data.local.entity.ScheduleEntity
import host.senk.dosenk.data.local.entity.UserEntity

@Dao
interface UserDao {

    // USUARIOS

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long // Devuelve el ID del nuevo usuario

    @androidx.room.Update
    suspend fun updateUser(user: UserEntity)

    @Query("SELECT * FROM users WHERE email = :identifier OR username = :identifier LIMIT 1")
    suspend fun getUserByEmailOrUsername(identifier: String): UserEntity?

    // HORARIOS -

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE userUuid = :userUuid")
    suspend fun getSchedulesForUser(userUuid: String): List<ScheduleEntity>

    // Borrar todo (útil para cerrar sesión limpia)
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()


    @Query("DELETE FROM schedules")
    suspend fun deleteAllSchedules()


}