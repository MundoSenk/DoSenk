package host.senk.dosenk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import host.senk.dosenk.data.local.entity.BlockProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: BlockProfileEntity)

    @Update
    suspend fun updateProfile(profile: BlockProfileEntity)

    @Query("SELECT * FROM block_profiles")
    fun getAllProfiles(): Flow<List<BlockProfileEntity>>

    // 🚨 CAMBIA ESTO (De id a uuid)
    @Query("SELECT * FROM block_profiles WHERE uuid = :uuid LIMIT 1")
    suspend fun getProfileByUuid(uuid: String): BlockProfileEntity?
}