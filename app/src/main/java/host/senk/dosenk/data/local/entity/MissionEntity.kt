package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "missions")
data class MissionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Lo que viene del Formulario
    val name: String,               // Estudiar Programación
    val durationMinutes: Int,       //  45
    val executionDate: Long,        // Timestamp (Cuándo toca sufrir)
    val assignmentType: String,     // Manual o Auto

    // Lo que viene de la Zona de Bloqueos
    val blockType: String,          // Humano, Dios

    // El motor del HomeFragment necesita saber esto:
    val status: String = "pending"  // Puede ser: "pending", "active", "completed", "failed"
)