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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import host.senk.dosenk.data.local.dao.BlockProfileDao


import host.senk.dosenk.data.local.UserPreferences
import java.util.UUID

@HiltViewModel
class CreateMissionViewModel @Inject constructor(
    val missionDao: MissionDao,
    private val blockProfileDao: BlockProfileDao,
    private val userPreferences: UserPreferences,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    // Slibgeton como mochilita para guardar datos antes de hacer una mision en la room

    var missionName = ""
    var missionDescription = ""

    var currentEditingMissionId: String? = null

    private var oldExecutionDate: Long = 0L ///ppara cancelar la alarma

    // Un canal de comunicación para avisarle a la pantalla que ya cargamos los datos
    private val _missionLoaded = MutableSharedFlow<MissionEntity>()
    val missionLoaded = _missionLoaded.asSharedFlow()

    private val _durationMinutes = MutableStateFlow(45)
    val durationMinutes: StateFlow<Int> = _durationMinutes

    private val _executionDate = MutableStateFlow<Long?>(null) //
    val executionDate: StateFlow<Long?> = _executionDate

    private val _assignmentType = MutableStateFlow("manual") //
    val assignmentType: StateFlow<String> = _assignmentType
    private val _startHour = MutableStateFlow<Int?>(null)
    private val _startMinute = MutableStateFlow<Int?>(null)


    val allCustomBlocks = blockProfileDao.getAllProfiles()

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


    fun isTimeValid(): Boolean {
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
        localCalendar.set(java.util.Calendar.MILLISECOND, 0)

        // ¿El momento que armó el usuario es mayor al segundo actual?
        return localCalendar.timeInMillis > System.currentTimeMillis()
    }


    // pa que nos e encimen las misiones
    suspend fun hasTimeConflict(): Boolean {
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
        localCalendar.set(java.util.Calendar.MILLISECOND, 0)

        val requestedStart = localCalendar.timeInMillis
        val requestedEnd = requestedStart + (durationMinutes.value * 60 * 1000L)

        // Traemos todas las misiones
        val allMissions = missionDao.getAllMissions().first()

        return allMissions.filter {
            // FILTRO: Que estén activas o pendientes, Y QUE NO SEA LA MISIÓN QUE ESTAMOS EDITANDO
            (it.status == "pending" || it.status == "active") && (it.uuid != currentEditingMissionId)
        }.any { mission ->
            val existingStart = mission.executionDate
            val existingEnd = existingStart + (mission.durationMinutes * 60 * 1000L)

            // Fórmula de colisión
            (requestedStart < existingEnd) && (requestedEnd > existingStart)
        }
    }


    fun saveMissionToDatabase(blockTypeChosen: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {

            val ownerUuid = userPreferences.userToken.first()


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
                uuid = currentEditingMissionId ?: UUID.randomUUID().toString(),
                userUuid = ownerUuid,
                name = missionName,
                description = missionDescription,
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
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }


            if (oldExecutionDate != 0L && oldExecutionDate != finalTimestamp) {
                val oldIntent = Intent(appContext, MissionTriggerReceiver::class.java)
                val oldPendingIntent = PendingIntent.getBroadcast(
                    appContext, (oldExecutionDate / 1000).toInt(), oldIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(oldPendingIntent)
            }


            // Creamos un intent pendiente único
            val pendingIntent = PendingIntent.getBroadcast(
                appContext,
                (finalTimestamp / 1000).toInt(), // ID único
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )



            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(finalTimestamp, null)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)

            } catch (e: SecurityException) {

            }

            if (currentEditingMissionId != null) {
                missionDao.updateMission(newMission)
            } else {
                missionDao.insertMission(newMission)
            }

            withContext(Dispatchers.Main) { onComplete() }
        }
    }


    fun loadMissionForEditing(missionId: String) {
        currentEditingMissionId = missionId
        viewModelScope.launch {
            val mission = missionDao.getMissionByUuid(missionId)
            if (mission != null) {
                missionName = mission.name
                missionDescription = mission.description
                oldExecutionDate = mission.executionDate

                _durationMinutes.value = mission.durationMinutes
                _executionDate.value = mission.executionDate
                _assignmentType.value = mission.assignmentType

                // Extraemos la hora y minuto para el reloj
                val calendar = java.util.Calendar.getInstance().apply { timeInMillis = mission.executionDate }
                _startHour.value = calendar.get(java.util.Calendar.HOUR_OF_DAY)
                _startMinute.value = calendar.get(java.util.Calendar.MINUTE)

                // Le avisamos a la UI que rellene los cuadros de texto
                _missionLoaded.emit(mission)
            }
        }
    }

}