package host.senk.dosenk.data.remote.model


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
    val multiplierApplied: Double
)

// El molde de la caja completa que mandamos al servidor
data class SyncMissionsRequest(
    val user_uuid: String, // Ojo aquí: el PHP lo espera con guion bajo
    val missions: List<MissionDto>
)