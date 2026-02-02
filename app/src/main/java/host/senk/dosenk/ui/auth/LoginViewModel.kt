package host.senk.dosenk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences // IMPORTANTE
import host.senk.dosenk.data.repository.AuthRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userPreferences: UserPreferences // <--- ¡ESTO FALTABA!
) : ViewModel() {

    // Canal para mandar eventos a la vista
    sealed class LoginEvent {
        object Success : LoginEvent()
        data class Error(val message: String) : LoginEvent()
    }

    private val _loginChannel = Channel<LoginEvent>()
    val loginEvent = _loginChannel.receiveAsFlow()

    fun onLoginClicked(user: String, pass: String) {
        viewModelScope.launch {
            if (user.isBlank() || pass.isBlank()) {
                _loginChannel.send(LoginEvent.Error("Llena todos los campos, gallo"))
                return@launch
            }

            // Llamada al repo (que ahora es híbrido Nube/Local)
            val result = repository.login(user, pass)

            val isSuccess = result.first
            val message = result.second

            if (isSuccess) {
                _loginChannel.send(LoginEvent.Success)
            } else {
                _loginChannel.send(LoginEvent.Error(message))
            }
        }
    }


    suspend fun isSetupFinished(): Boolean {

        return userPreferences.isSetupFinished.first()
    }
}