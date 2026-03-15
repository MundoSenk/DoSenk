package host.senk.dosenk.data.remote.model

// 🚨 1. Regresamos rankName a String
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

    // 🚨 2. Las listas que nos manda PHP
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