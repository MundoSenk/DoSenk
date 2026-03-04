package host.senk.dosenk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.dao.MissionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class MissionCardState {
    object Idle : MissionCardState()
    data class Pending(val secondsUntilStart: Int, val missionName: String) : MissionCardState()
    data class Active(val secondsLeft: Int, val blockType: String) : MissionCardState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val userDao: UserDao,
    private val missionDao: MissionDao
) : ViewModel() {

    val currentUserAlias = userPreferences.userAlias.asLiveData()
    val isEmergencyActive = userPreferences.isEmergencyActive.asLiveData()

    // Variable reactiva para el rango
    private val _realRankName = MutableStateFlow("Desconocido")
    val realRankName: StateFlow<String> = _realRankName

    // El estado de la tarjeta de misiones que hicimos antes
    private val _missionState = MutableStateFlow<MissionCardState>(MissionCardState.Idle)
    val missionState: StateFlow<MissionCardState> = _missionState

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

    private fun checkCurrentMissions() {
        viewModelScope.launch {
            // El Flow se queda escuchando 24/7 si hay cambios en la tabla
            missionDao.getNextPendingMission().collect { mission ->
                if (mission != null) {

                    val currentTime = System.currentTimeMillis()
                    val diffMillis = mission.executionDate - currentTime

                    val secondsUntilStart = if (diffMillis > 0) (diffMillis / 1000).toInt() else 0


                    _missionState.value = MissionCardState.Pending(
                        secondsUntilStart = secondsUntilStart,
                        missionName = mission.name
                    )
                } else {
                    _missionState.value = MissionCardState.Idle
                }
            }
        }
    }
}