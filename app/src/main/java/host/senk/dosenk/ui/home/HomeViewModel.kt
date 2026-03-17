package host.senk.dosenk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.dao.MissionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext //
import host.senk.dosenk.service.MissionBlockerService


import host.senk.dosenk.data.local.dao.BlockProfileDao //

sealed class MissionCardState {
    object Idle : MissionCardState()
    data class Pending(val secondsUntilStart: Int, val missionName: String) : MissionCardState()
    data class Active(val secondsLeft: Int, val blockType: String) : MissionCardState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userDao: UserDao,
    private val missionDao: MissionDao,
    private val blockProfileDao: BlockProfileDao,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val currentUserAlias = userPreferences.userAlias.asLiveData()
    val isEmergencyActive = userPreferences.isEmergencyActive.asLiveData()

    private val _realRankName = MutableStateFlow("Desconocido")
    val realRankName: StateFlow<String> = _realRankName

    private val _missionState = MutableStateFlow<MissionCardState>(MissionCardState.Idle)
    val missionState: StateFlow<MissionCardState> = _missionState

    // Esta es nuestra "bomba de tiempo"
    private var transitionJob: Job? = null

    init {
        loadUserRank()
        checkCurrentMissions()
    }

    private fun loadUserRank() {
        viewModelScope.launch {
            val alias = userPreferences.userAlias.first()
            val userEntity = userDao.getUserByEmailOrUsername(alias)
            if (userEntity != null) {
                _realRankName.value = userEntity.rankName
            }
        }
    }


    // EL CEREBRO Pending -> Active -> Completed

    private fun checkCurrentMissions() {
        viewModelScope.launch {
            // Escuchamos AL MISMO TIEMPO si hay una misión Activa o una Pendiente
            combine(
                missionDao.getActiveMission(),
                missionDao.getNextPendingMission()
            ) { active, pending ->
                Pair(active, pending)
            }.collect { (active, pending) ->

                // Cancelamos cualquier reloj anterior para que no choquen
                transitionJob?.cancel()

                val currentTime = System.currentTimeMillis()

                if (active != null) {
                    // HAY UNA MISIÓN ACTIVA
                    val endTime = active.executionDate + (active.durationMinutes * 60 * 1000L)
                    val timeLeftMillis = endTime - currentTime

                    if (timeLeftMillis > 0) {
                        val remainingSeconds = (timeLeftMillis / 1000).toInt()

                        _missionState.value = MissionCardState.Active(
                            secondsLeft = remainingSeconds,
                            blockType = active.blockType
                        )

                        // PREPARAMOS LAS BALAS PARA EL CADENERO
                        val serviceIntent = Intent(appContext, MissionBlockerService::class.java).apply {
                            putExtra("DURATION_SECONDS", remainingSeconds)
                            putExtra("MISSION_NAME", active.name)
                            putExtra("IS_TIME_PUNISHMENT", false)

                            // Le mandamos el tipo de bloqueo
                            putExtra("BLOCK_TYPE", active.blockType)
                        }

                        //  SI ES PERSONALIZADO, LE CARGAMOS LA LISTA NEGRA DESDE ROOM
                        if (active.blockType != "Dios" && active.blockType != "Humano") {
                            // Como estamos dentro de una corrutina (collect), podemos usar first() para leer rápido
                            val profile = blockProfileDao.getAllProfiles().first().find { it.name == active.blockType }
                            val jsonList = profile?.blockedAppsJson ?: "[]"
                            serviceIntent.putExtra("BLOCK_LIST_JSON", jsonList)
                        }

                        // LANZAMOS EL BLOQUEO INMEDIATO PARA REANUDAR EL CASTIGO
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) appContext.startForegroundService(serviceIntent)
                            else appContext.startService(serviceIntent)
                        } catch (e: Exception) {
                            if (e.javaClass.simpleName == "ForegroundServiceStartNotAllowedException") appContext.startService(serviceIntent)
                        }

                        transitionJob = viewModelScope.launch {
                            delay(timeLeftMillis)
                            archiveMission(active)
                        }
                    } else {
                        archiveMission(active)
                    }

                } else if (pending != null) {
                    // HAY UNA MISIÓN PENDIENTE (ESPERANDO)
                    val timeToStartMillis = pending.executionDate - currentTime

                    if (timeToStartMillis > 0) {
                        // Le pasamos los datos a tu MissionTimerManager para que cuente los minutos que faltan
                        _missionState.value = MissionCardState.Pending(
                            secondsUntilStart = (timeToStartMillis / 1000).toInt(),
                            missionName = pending.name
                        )

                        // Programamos la bomba: Cuando el tiempo llegue a cero, ACTÍVALA
                        transitionJob = viewModelScope.launch {
                            delay(timeToStartMillis)
                            activateMission(pending)
                        }
                    } else {
                        // Si el tiempo ya pasó o es cero, actívala inmediatamente
                        activateMission(pending)
                    }

                } else {
                    _missionState.value = MissionCardState.Idle
                }
            }
        }
    }

    private fun activateMission(mission: host.senk.dosenk.data.local.entity.MissionEntity) {
        viewModelScope.launch(Dispatchers.IO) {

            val updatedMission = mission.copy(status = "active")
            missionDao.updateMission(updatedMission)

            val serviceIntent = Intent(appContext, MissionBlockerService::class.java)

            val isAutoTime = android.provider.Settings.Global.getInt(
                appContext.contentResolver,
                android.provider.Settings.Global.AUTO_TIME, 0
            ) == 1

            if (!isAutoTime) {
                serviceIntent.putExtra("DURATION_SECONDS", 99999)
                serviceIntent.putExtra("MISSION_NAME", "¡Trampa!\nPrende la Hora Automática.")
                serviceIntent.putExtra("IS_TIME_PUNISHMENT", true)
            } else {
                val durationSeconds = mission.durationMinutes * 60
                serviceIntent.putExtra("DURATION_SECONDS", durationSeconds)
                serviceIntent.putExtra("MISSION_NAME", mission.name)
                serviceIntent.putExtra("IS_TIME_PUNISHMENT", false)

                // LE MANDAMOS EL TIPO DE BLOQUEO
                serviceIntent.putExtra("BLOCK_TYPE", mission.blockType)

                // SI ES PERSONALIZADO, BUSCAMOS LA LISTA NEGRA EN ROOM Y SE LA MANDAMOS
                if (mission.blockType != "Dios" && mission.blockType != "Humano") {
                    val profile = blockProfileDao.getAllProfiles().first().find { it.name == mission.blockType }
                    val jsonList = profile?.blockedAppsJson ?: "[]"
                    serviceIntent.putExtra("BLOCK_LIST_JSON", jsonList)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    appContext.startForegroundService(serviceIntent)
                } else {
                    appContext.startService(serviceIntent)
                }
            } catch (e: Exception) {
                if (e.javaClass.simpleName == "ForegroundServiceStartNotAllowedException") {
                    appContext.startService(serviceIntent)
                }
            }
        }
    }

    private fun archiveMission(mission: host.senk.dosenk.data.local.entity.MissionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMission = mission.copy(status = "completed")
            missionDao.updateMission(updatedMission)

            // Apagamos el NUEVO servicio
            val serviceIntent = Intent(appContext, MissionBlockerService::class.java)
            appContext.stopService(serviceIntent)
        }
    }
}