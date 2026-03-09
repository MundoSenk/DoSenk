package host.senk.dosenk.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.dao.MissionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val missionDao: MissionDao
) : ViewModel() {

    private val _timelineItems = MutableStateFlow<List<TimelineItem>>(emptyList())
    val timelineItems: StateFlow<List<TimelineItem>> = _timelineItems

    init {
        loadMissionsForToday()
    }

    private fun loadMissionsForToday() {
        viewModelScope.launch {
            missionDao.getAllMissions().collect { rawMissions ->

                // Solo trae misiones (De 00:00:00 a 23:59:59)
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.timeInMillis
                val endOfDay = startOfDay + 86400000L - 1 // 24 horas menos 1 milisegundo

                // Solo agarramos las que caen en el rango de HOY
                val todaysMissions = rawMissions.filter {
                    it.executionDate in startOfDay..endOfDay
                }.sortedBy { it.executionDate }

                val newTimeline = mutableListOf<TimelineItem>()
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                for (i in todaysMissions.indices) {
                    val currentMission = todaysMissions[i]

                    // Agregamos la misión real
                    newTimeline.add(
                        TimelineItem.MissionCard(
                            timeLabel = timeFormat.format(currentMission.executionDate),
                            mission = currentMission
                        )
                    )

                    // Calcular huecos (
                    if (i < todaysMissions.size - 1) {
                        val nextMission = todaysMissions[i + 1]
                        val currentEndTimeMillis = currentMission.executionDate + (currentMission.durationMinutes * 60 * 1000L)
                        val gapMillis = nextMission.executionDate - currentEndTimeMillis
                        val gapMinutes = (gapMillis / (1000 * 60)).toInt()

                        if (gapMinutes > 0) {
                            newTimeline.add(
                                TimelineItem.EmptySlot(
                                    timeLabel = timeFormat.format(currentEndTimeMillis),
                                    durationMinutes = gapMinutes
                                )
                            )
                        }
                    }
                }

                _timelineItems.value = newTimeline
            }
        }
    }
}