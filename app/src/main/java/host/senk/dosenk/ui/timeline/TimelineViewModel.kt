package host.senk.dosenk.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.MissionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val missionDao: MissionDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timelineItems: StateFlow<List<TimelineItem>> = _timelineItems

    private val _weekOffset = MutableStateFlow(0)

    val currentUserAlias = userPreferences.userAlias.asLiveData()

    init {
        loadMissionsForToday()
    }

    // RECIBE EL COMANDO DE LA FLECHA DESDE EL FRAGMENTO
    fun setWeekOffset(offset: Int) {
        _weekOffset.value = offset
    }

    //  Si la DB cambia o el offset cambia, esto se recalcula automático
    val weeklyItems: Flow<List<WeeklyCardItem>> = combine(
        missionDao.getAllMissions(),
        _weekOffset
    ) { rawMissions, offset ->

        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.add(Calendar.WEEK_OF_YEAR, offset)

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfWeek = calendar.timeInMillis
        val weekCards = mutableListOf<WeeklyCardItem>()
        val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

        for (i in 0..6) {
            val dayStart = startOfWeek + (i * 86400000L)
            val dayEnd = dayStart + 86400000L - 1

            val calDay = Calendar.getInstance().apply { timeInMillis = dayStart }

            val dayNumberStr = calDay.get(Calendar.DAY_OF_MONTH).toString()
            val monthName = calDay.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("es", "ES"))?.replaceFirstChar { it.uppercase() }
            val year = calDay.get(Calendar.YEAR)
            val monthYearStr = "de $monthName, $year"

            // Filtramos las misiones del día
            val dayMissions = rawMissions.filter { it.executionDate in dayStart..dayEnd }

            val isToday = (Calendar.getInstance().timeInMillis in dayStart..dayEnd)

            //  CÁLCULO DE XP (1 min = 1 XP)
            val totalDayXp = dayMissions.sumOf { it.durationMinutes }

            // Tomamos las 3 más largas (Más XP)
            val topMissions = dayMissions
                .sortedByDescending { it.durationMinutes }
                .take(3)
                .map { "${it.durationMinutes} Xp   ${it.name}" }

            weekCards.add(
                WeeklyCardItem(
                    timestamp = dayStart,
                    dayName = dayNames[i],
                    dayNumber = dayNumberStr,
                    monthYear = monthYearStr,
                    isToday = isToday,
                    totalMissions = dayMissions.size,
                    totalProjects = 0, // TODO: Cambiar cuando se agregue la entidad Proyecto
                    dayXp = totalDayXp,
                    importantMissions = topMissions
                )
            )
        }
        weekCards
    }

    fun loadMissionsForToday(customTimestamp: Long? = null) {
        viewModelScope.launch {
            missionDao.getAllMissions().collect { rawMissions ->

                val calendar = Calendar.getInstance()

                if (customTimestamp != null) {
                    calendar.timeInMillis = customTimestamp
                }

                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val endOfDay = startOfDay + 86400000L - 1

                val todaysMissions = rawMissions.filter {
                    it.executionDate in startOfDay..endOfDay
                }.sortedBy { it.executionDate }

                val newTimeline = mutableListOf<TimelineItem>()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                for (i in todaysMissions.indices) {
                    val currentMission = todaysMissions[i]
                    newTimeline.add(TimelineItem.MissionCard(timeFormat.format(currentMission.executionDate), currentMission))

                    if (i < todaysMissions.size - 1) {
                        val nextMission = todaysMissions[i + 1]
                        val currentEndTimeMillis = currentMission.executionDate + (currentMission.durationMinutes * 60 * 1000L)
                        val gapMillis = nextMission.executionDate - currentEndTimeMillis
                        val gapMinutes = (gapMillis / (1000 * 60)).toInt()

                        if (gapMinutes > 0) {
                            newTimeline.add(TimelineItem.EmptySlot(timeFormat.format(currentEndTimeMillis), gapMinutes))
                        }
                    }
                }
                _timelineItems.value = newTimeline
            }
        }
    }
}