package host.senk.dosenk.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TutorialMissionViewModel @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _realRankName = MutableStateFlow("Desconocido")
    val realRankName: StateFlow<String> = _realRankName

    private val _realAlias = MutableStateFlow("@User")
    val realAlias: StateFlow<String> = _realAlias

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            // 1. Extraemos el nombre de DataStore
            val alias = userPreferences.userAlias.first()
            _realAlias.value = alias

            // 2. Extraemos el rango de Room usando el nombre
            val userEntity = userDao.getUserByEmailOrUsername(alias)
            if (userEntity != null) {
                _realRankName.value = userEntity.rankName
            }
        }
    }

    // Cuando termine el tutorial, le damos la libertad total (Stage 3)
    fun finishOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferences.saveSetupFinished(3)
            onFinished()
        }
    }
}