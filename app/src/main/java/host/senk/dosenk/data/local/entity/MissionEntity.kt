package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val userUuid: String,

    // Lo que viene del Formulario
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val executionDate: Long,
    val assignmentType: String,

    // Lo que viene de la Zona de Bloqueos
    val blockType: String,

    val status: String = "pending",


    val potentialXp: Int = 0,         // Lo que se le prometió en el ticket
    val earnedXp: Int = 0,            // Lo que realmente ganó al terminar (0 si falla)
    val multiplierApplied: Double = 1.0 // El multiplicador que se usó (para historial)
)