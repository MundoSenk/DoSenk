package host.senk.dosenk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import host.senk.dosenk.data.local.entity.MissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    // Guardar una nueva misión sádica
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity): Long

    // Actualizar estado ( pasar de pending a active)
    @Update
    suspend fun updateMission(mission: MissionEntity)

    // Obtener la misión activa (Si hay una, el celular debe estar bloqueado)
    @Query("SELECT * FROM missions WHERE status = 'active' LIMIT 1")
    fun getActiveMission(): Flow<MissionEntity?>

    //  Obtener la PRÓXIMA misión pendiente
    // Las ordenamos por fecha, para agarrar la que esté más cerquita
    @Query("SELECT * FROM missions WHERE status = 'pending' ORDER BY executionDate ASC LIMIT 1")
    fun getNextPendingMission(): Flow<MissionEntity?>

    // Historial
    @Query("SELECT * FROM missions ORDER BY executionDate DESC")
    fun getAllMissions(): Flow<List<MissionEntity>>
}