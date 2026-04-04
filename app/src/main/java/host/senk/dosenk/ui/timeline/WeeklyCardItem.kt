package host.senk.dosenk.ui.timeline

// Los datos que necesita cada tarjeta de la semana
data class WeeklyCardItem(
    val dayName: String,
    val dateDay: String,
    val isToday: Boolean,
    val totalMissions: Int,
    val totalProjects: Int,
    val importantMissions: List<String>
)