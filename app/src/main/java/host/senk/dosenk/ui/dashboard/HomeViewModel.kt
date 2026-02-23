package host.senk.dosenk.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {



    val currentUserAlias = userPreferences.userAlias.asLiveData()


    val isEmergencyActive = userPreferences.isEmergencyActive.asLiveData()

    fun toggleEmergencyMode() {
        viewModelScope.launch {
            // Leemos el valor actual e invertimos (True -> False, False -> True)
            val current = userPreferences.isEmergencyActive.first()
            userPreferences.saveEmergencyMode(!current)
        }
    }

}