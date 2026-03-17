package host.senk.dosenk.data.remote.model

// 🚨  Regresamos rankName a String
data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val uuid: String?,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val birthDate: String?,
    val themeColor: String?,
    val setupFinished: Int?,
    val dailyWastedHours: Float?,
    val rankName: String?,

    // Las listas que nos manda PHP
    val schedules: List<ScheduleDto>?,
    val blockProfiles: List<BlockProfileDto>?
)

// Moldes chiquitos para las listas
data class ScheduleDto(
    val type: String,
    val gridJson: String
)

data class BlockProfileDto(
    val name: String,
    val blockedAppsJson: String
)


data class SyncBlocksRequest(
    val uuid: String,
    val blocks: List<BlockProfileDto>
)


