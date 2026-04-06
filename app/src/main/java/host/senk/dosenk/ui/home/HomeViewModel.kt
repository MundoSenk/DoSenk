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
import host.senk.dosenk.domain.MissionCloneManager
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import host.senk.dosenk.service.MissionBlockerService
import host.senk.dosenk.data.local.dao.BlockProfileDao
import kotlinx.coroutines.flow.map


import host.senk.dosenk.data.repository.AuthRepository

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
    private val repository: AuthRepository,
    private val cloneManager: MissionCloneManager,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val currentUserAlias = userPreferences.userAlias.asLiveData()
    val isEmergencyActive = userPreferences.isEmergencyActive.asLiveData()

    private val _realRankName = MutableStateFlow("Desconocido")
    val realRankName: StateFlow<String> = _realRankName

    private val _missionState = MutableStateFlow<MissionCardState>(MissionCardState.Idle)
    val missionState: StateFlow<MissionCardState> = _missionState
    val unclaimedMission = missionDao.getFirstUnclaimedMission().asLiveData()

    // LA EDAD DE LA CUENTA (DÍAS CON >DO)
    val diasConDo = userPreferences.startDateMs.map { startMs ->
        if (startMs == 0L) {
            0
        } else {
            val diffMs = System.currentTimeMillis() - startMs
            (diffMs / (1000 * 60 * 60 * 24)).toInt()
        }
    }.asLiveData()

    // Esta es nuestra "bomba de tiempo"
    private var transitionJob: Job? = null

    init {
        loadUserRank()

        viewModelScope.launch {
            cloneManager.generateClonesForNext7Days()

            repository.syncMissionsToCloud()

            checkCurrentMissions()
        }

        checkAndUpdateDailyStreak()


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



    private fun checkAndUpdateDailyStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            // Asumiendo que en tu UserDao tienes la función getActiveUser()
            val user = userDao.getActiveUser().first() ?: return@launch

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val todayStr = sdf.format(java.util.Date())

            // Si ya revisamos la racha hoy, no hacemos nada
            if (user.lastLoginDate == todayStr) {
                return@launch
            }

            var newStreak = user.streakDays

            if (user.lastLoginDate.isNotEmpty()) {
                try {
                    val lastDate = sdf.parse(user.lastLoginDate)
                    val todayDate = sdf.parse(todayStr)

                    // Calculamos la diferencia en milisegundos y la pasamos a días
                    val diffMillis = todayDate.time - lastDate.time
                    val diffDays = diffMillis / (1000 * 60 * 60 * 24)

                    when {
                        diffDays == 1L -> newStreak += 1 // Entró ayer, ¡felicidades! Suma 1.
                        diffDays > 1L -> newStreak = 1   // Perdió un día, racha rota.
                        else -> {} // Caso extraño (viajes en el tiempo), no hacemos nada
                    }
                } catch (e: Exception) {
                    newStreak = 1
                }
            } else {
                newStreak = 1
            }

            // Guardamos la nueva racha y la fecha de hoy en Room local
            val updatedUser = user.copy(
                streakDays = newStreak,
                lastLoginDate = todayStr
            )
            userDao.updateUser(updatedUser)

            //
            repository.syncStatsToCloud(updatedUser.currentXp, newStreak)
        }
    }



    // EL CEREBRO Pending -> Active -> Completed
    private fun checkCurrentMissions() {
        viewModelScope.launch {
            combine(
                missionDao.getActiveMission(),
                missionDao.getNextPendingMission()
            ) { active, pending -> Pair(active, pending)
            }.collect { (active, pending) ->

                transitionJob?.cancel()
                val currentTime = System.currentTimeMillis()

                if (active != null) {
                    // HAY UNA MISIÓN ACTIVA
                    val endTime = active.executionDate + (active.durationMinutes * 60 * 1000L)
                    val timeLeftMillis = endTime - currentTime

                    //  ESCUDO ANTI-CRASHEOS: Solo prendemos el servicio si le quedan más de 2 segundos
                    if (timeLeftMillis > 2000) {
                        val remainingSeconds = (timeLeftMillis / 1000).toInt()

                        _missionState.value = MissionCardState.Active(
                            secondsLeft = remainingSeconds,
                            blockType = active.blockType
                        )

                        val serviceIntent = Intent(appContext, MissionBlockerService::class.java).apply {
                            putExtra("DURATION_SECONDS", remainingSeconds)
                            putExtra("MISSION_NAME", active.name)
                            putExtra("BLOCK_TYPE", active.blockType)

                            val isAutoTime = android.provider.Settings.Global.getInt(appContext.contentResolver, android.provider.Settings.Global.AUTO_TIME, 0) == 1
                            if (!isAutoTime) {
                                putExtra("IS_TIME_PUNISHMENT", true)
                                putExtra("DURATION_SECONDS", 99999)
                                putExtra("MISSION_NAME", "¡Trampa! Prende la Hora Automática.")
                            } else {
                                putExtra("IS_TIME_PUNISHMENT", false)
                            }
                        }

                        if (active.blockType != "Dios" && active.blockType != "Humano") {
                            val profile = blockProfileDao.getAllProfiles().first().find { it.name == active.blockType }
                            serviceIntent.putExtra("BLOCK_LIST_JSON", profile?.blockedAppsJson ?: "[]")
                        }

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
                    val timeToStartMillis = pending.executionDate - currentTime
                    if (timeToStartMillis > 0) {
                        _missionState.value = MissionCardState.Pending(
                            secondsUntilStart = (timeToStartMillis / 1000).toInt(),
                            missionName = pending.name
                        )
                        transitionJob = viewModelScope.launch {
                            delay(timeToStartMillis)
                            activateMission(pending)
                        }
                    } else {
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

        }
    }

    private fun archiveMission(mission: host.senk.dosenk.data.local.entity.MissionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMission = mission.copy(
                status = "completed",
                earnedXp = mission.potentialXp
            )
            missionDao.updateMission(updatedMission)

            // Apagamos el NUEVO servicio
            val serviceIntent = Intent(appContext, MissionBlockerService::class.java)
            appContext.stopService(serviceIntent)


            val user = userDao.getActiveUser().first()
            if (user != null) {
                repository.syncStatsToCloud(user.currentXp, user.streakDays)
            }

            repository.syncMissionsToCloud()
        }
    }
}