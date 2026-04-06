package host.senk.dosenk.data.remote.model


// Actualizamos MissionDto para que tenga los campos nuevos
data class MissionDto(
    val uuid: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val executionDate: Long,
    val assignmentType: String,
    val blockType: String,
    val status: String,
    val potentialXp: Int,
    val earnedXp: Int,
    val multiplierApplied: Double,
    val isReclaimed: Boolean,
    val templateUuid: String?,
    val isManualOverride: Boolean
)

// Creamos el molde para las Rutinas (Plantillas)
data class MissionTemplateDto(
    val uuid: String,
    val name: String,
    val description: String,
    val durationMinutes: Int,
    val daysOfWeek: List<Int>,
    val startTimeMin: Int,
    val assignmentType: String,
    val blockType: String,
    val potentialXp: Int,
    val isActive: Boolean
)

// Actualizamos la caja de envío para mandar ambas listas
data class SyncMissionsRequest(
    val user_uuid: String,
    val missions: List<MissionDto>,
    val templates: List<MissionTemplateDto>
)