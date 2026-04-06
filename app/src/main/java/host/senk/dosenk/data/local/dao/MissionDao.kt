package host.senk.dosenk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import host.senk.dosenk.data.local.entity.MissionEntity
import host.senk.dosenk.data.local.entity.MissionTemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MissionDao {

    // Guardar una nueva misión sádica
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMission(mission: MissionEntity): Long


    // Guardar una nueva PLANTILLA (La Rutina repetitiva)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MissionTemplateEntity): Long

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




    @Query("SELECT * FROM missions WHERE uuid = :uuid LIMIT 1")
    suspend fun getMissionByUuid(uuid: String): MissionEntity?


    @Query("UPDATE missions SET blockType = :newBlockName WHERE blockType = :oldBlockName AND status = 'pending'")
    suspend fun reassignMissions(oldBlockName: String, newBlockName: String)



    @Query("SELECT * FROM missions WHERE status = 'pending' AND name = :missionName LIMIT 1")
    suspend fun getPendingMissionByNameFast(missionName: String): MissionEntity?


    @Query("SELECT * FROM missions WHERE status = 'active' LIMIT 1")
    suspend fun getActiveMissionFast(): MissionEntity?

    @Query("SELECT * FROM missions WHERE status = 'pending' ORDER BY executionDate ASC LIMIT 1")
    suspend fun getLatestPendingMissionFast(): MissionEntity?


    @Query("SELECT * FROM missions WHERE status = 'completed' AND isReclaimed = 0 ORDER BY executionDate ASC LIMIT 1")
    fun getFirstUnclaimedMission(): Flow<MissionEntity?>

    @Query("UPDATE missions SET isReclaimed = 1 WHERE uuid = :uuid")
    suspend fun markAsReclaimed(uuid: String)


    @Query("""
        SELECT * FROM missions 
        WHERE status IN ('pending', 'active') 
        AND executionDate < :requestedEndMs 
        AND (executionDate + (durationMinutes * 60000)) > :requestedStartMs
    """)
    suspend fun getCollidingMissions(requestedStartMs: Long, requestedEndMs: Long): List<MissionEntity>

    // Trae todas las plantillas activas
    @Query("SELECT * FROM mission_templates WHERE isActive = 1")
    suspend fun getActiveTemplates(): List<MissionTemplateEntity>


    @Query("SELECT * FROM mission_templates WHERE uuid = :uuid LIMIT 1")
    suspend fun getTemplateById(uuid: String): MissionTemplateEntity?

    @Update
    suspend fun updateTemplate(template: MissionTemplateEntity)









    // EL BORRADO LÓGICO (Lápidas)
    @Query("UPDATE missions SET status = 'deleted' WHERE uuid = :uuid")
    suspend fun deleteMissionByUuid(uuid: String)

    @Query("UPDATE mission_templates SET isActive = 0 WHERE uuid = :uuid")
    suspend fun deleteTemplateByUuid(uuid: String)

    @Query("UPDATE missions SET status = 'deleted' WHERE templateUuid = :templateId AND status = 'pending'")
    suspend fun deletePendingClonesByTemplate(templateId: String)

    //  ESCONDER LOS MUERTOS DE LA APP
    @Query("SELECT * FROM missions WHERE status != 'deleted' ORDER BY executionDate DESC")
    fun getAllMissions(): Flow<List<MissionEntity>>

    //  LOS RECOLECTORES DE CADÁVERES (Para AuthRepository)
    @Query("SELECT * FROM missions")
    suspend fun getAllMissionsForSync(): List<MissionEntity>

    @Query("SELECT * FROM mission_templates")
    suspend fun getAllTemplatesForSync(): List<MissionTemplateEntity>







}