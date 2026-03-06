package host.senk.dosenk.ui.mission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.dao.MissionDao
import host.senk.dosenk.data.local.entity.MissionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import host.senk.dosenk.service.MissionTriggerReceiver

@HiltViewModel
class CreateMissionViewModel @Inject constructor(
    private val missionDao: MissionDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // Slibgeton como mochilita para guardar datos antes de hacer una mision en la room

    var missionName = ""

    private val _durationMinutes = MutableStateFlow(45)
    val durationMinutes: StateFlow<Int> = _durationMinutes

    private val _executionDate = MutableStateFlow<Long?>(null) //
    val executionDate: StateFlow<Long?> = _executionDate

    private val _assignmentType = MutableStateFlow("manual") //
    val assignmentType: StateFlow<String> = _assignmentType
    private val _startHour = MutableStateFlow<Int?>(null)
    private val _startMinute = MutableStateFlow<Int?>(null)

    // FUNCIONES PARA ACTUALIZAR LA MOCHILA

    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
    }

    fun setExecutionDate(timestamp: Long) {
        _executionDate.value = timestamp
    }

    fun setAssignmentType(type: String) {
        _assignmentType.value = type
    }

    fun setStartTime(hour: Int, minute: Int) {
        _startHour.value = hour
        _startMinute.value = minute
    }

    // Validar antes de pasar a la Zona de Bloqueos
    fun isFormValid(): Boolean {
        return missionName.isNotBlank() &&
                _executionDate.value != null &&
                _startHour.value != null
    }


    fun saveMissionToDatabase(blockTypeChosen: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {

            // Unimos el Día (del calendario UTC) con la Hora local
            val utcDate = executionDate.value ?: System.currentTimeMillis()
            val calendarUTC = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            calendarUTC.timeInMillis = utcDate

            val localCalendar = java.util.Calendar.getInstance()
            localCalendar.set(java.util.Calendar.YEAR, calendarUTC.get(java.util.Calendar.YEAR))
            localCalendar.set(java.util.Calendar.MONTH, calendarUTC.get(java.util.Calendar.MONTH))
            localCalendar.set(java.util.Calendar.DAY_OF_MONTH, calendarUTC.get(java.util.Calendar.DAY_OF_MONTH))
            localCalendar.set(java.util.Calendar.HOUR_OF_DAY, _startHour.value ?: 0)
            localCalendar.set(java.util.Calendar.MINUTE, _startMinute.value ?: 0)
            localCalendar.set(java.util.Calendar.SECOND, 0)

            val finalTimestamp = localCalendar.timeInMillis

            val newMission = MissionEntity(
                name = missionName,
                durationMinutes = durationMinutes.value,
                executionDate = finalTimestamp,
                assignmentType = assignmentType.value,
                blockType = blockTypeChosen,
                status = "pending"
            )


            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(appContext, MissionTriggerReceiver::class.java).apply {
                putExtra("MISSION_NAME", missionName)
                putExtra("DURATION_MINUTES", durationMinutes.value)
            }


            // Creamos un intent pendiente único
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                (finalTimestamp / 1000).toInt(), // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )



            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    finalTimestamp,
                    pendingIntent
                )
            } catch (e: SecurityException) {

            }

            missionDao.insertMission(newMission)

            withContext(Dispatchers.Main) { onComplete() }
        }
    }

}