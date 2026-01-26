package host.senk.dosenk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import host.senk.dosenk.data.local.entity.ScheduleEntity
import host.senk.dosenk.data.local.entity.UserEntity

@Dao
interface UserDao {

    // --- USUARIOS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long // Devuelve el ID del nuevo usuario

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    // --- HORARIOS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity)

    @Query("SELECT * FROM schedules WHERE userId = :userId")
    suspend fun getSchedulesForUser(userId: Int): List<ScheduleEntity>

    // Borrar todo (útil para cerrar sesión limpia)
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}