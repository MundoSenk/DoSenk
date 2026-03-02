package host.senk.dosenk.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import kotlinx.coroutines.Dispatchers
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


    fun saveSetupStage(stage: Int) {
        viewModelScope.launch {
            userPreferences.saveSetupFinished(stage)
        }
    }


    fun saveAppTheme(themeName: String) {

        viewModelScope.launch(Dispatchers.IO) {

            //  TRADUCIR A NÚMERO PARA DATASTORE
            val themeIndex = when(themeName) {
                "red" -> 1
                "dark" -> 2
                "teal" -> 3
                else -> 0 //
            }

            // GUARDAR EN DATASTORE (Preferencias)
            userPreferences.saveTheme(themeIndex)

            // GUARDAR EN ROOM (Base de datos local)
            val currentAlias = realAlias.value
            val userEntity = userDao.getUserByEmailOrUsername(currentAlias)

            if (userEntity != null) {
                val updatedUser = userEntity.copy(themeColor = themeName)
                userDao.updateUser(updatedUser)
            }

            // GUARDAR EN PHP TODO LUEGO CON MAS CALMA
            try {
                ///repository.updateThemeInServer(currentAlias, themeName)
            } catch (e: Exception) {

                e.printStackTrace()
            }
        }
    }



    // Cuando termine el tutorial, le damos la libertad total (Stage 4)
    fun finishOnboarding(onFinished: () -> Unit) {
        viewModelScope.launch {
            userPreferences.saveSetupFinished(4)
            onFinished()
        }
    }
}