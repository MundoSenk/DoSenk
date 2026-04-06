package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

// Rutina recurrente
@Entity(tableName = "mission_templates")
data class MissionTemplateEntity(
    @PrimaryKey val uuid: String = java.util.UUID.randomUUID().toString(),
    val userUuid: String,
    val name: String,
    val description: String = "",
    val durationMinutes: Int,

    // Los días que se repite (ej. [2, 4, 6] para Lunes, Miércoles, Viernes) -> Requiere TypeConverter a JSON
    val daysOfWeek: List<Int>,

    // Minutos desde la medianoche (ej. 8:30 AM = 510). ¡Súper fácil para comparar colisiones!
    val startTimeMin: Int,

    val assignmentType: String,
    val blockType: String,
    val potentialXp: Int = 0,
    val isActive: Boolean = true // Por si el usuario pausa la rutina
)

// EL HIJO
@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val userUuid: String,
    val name: String,
    val description: String = "",
    val durationMinutes: Int,
    val executionDate: Long, // El Timestamp exacto (Día y Hora)
    val assignmentType: String,
    val blockType: String,
    val status: String = "pending",
    val potentialXp: Int = 0,
    val earnedXp: Int = 0,
    val multiplierApplied: Double = 1.0,
    val isReclaimed: Boolean = false,


    val templateUuid: String? = null,
    val isManualOverride: Boolean = false
)