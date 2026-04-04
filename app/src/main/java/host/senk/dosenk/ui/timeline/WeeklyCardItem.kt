package host.senk.dosenk.ui.timeline

// Los datos que necesita cada tarjeta de la semana
data class WeeklyCardItem(
    val timestamp: Long,
    val dayName: String,
    val dayNumber: String,
    val monthYear: String,
    val isToday: Boolean,
    val totalMissions: Int,
    val totalProjects: Int,
    val dayXp: Int,
    val importantMissions: List<String>
)