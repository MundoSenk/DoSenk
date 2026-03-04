package host.senk.dosenk.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
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
    private val userDao: UserDao //
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
            // TODO: Aquí leeremos la tabla de 'missions' en Room.
            // Lo dejamos en Idle (Vacío) por ahora para que no truene,
            // ya pronto le inyectaremos las misiones reales de la base de datos.
            _missionState.value = MissionCardState.Idle
        }
    }
}