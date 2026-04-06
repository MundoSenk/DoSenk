package host.senk.dosenk.data.remote.model


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
    val blockProfiles: List<BlockProfileDto>?,
    val currentXp: Int?,
    val streakDays: Int?,
    val createdAt: String?
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

data class SyncStatsRequest(
    val uuid: String,
    val current_xp: Int,
    val streak_days: Int
)






