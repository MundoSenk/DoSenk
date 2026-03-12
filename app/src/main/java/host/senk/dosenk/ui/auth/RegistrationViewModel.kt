package host.senk.dosenk.ui.auth

import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Patterns

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val _navigateToVerify = MutableLiveData<String>()
    val navigateToVerify: LiveData<String> = _navigateToVerify

    private val _userSkinIndex = MutableLiveData(0)
    val userSkinIndex: LiveData<Int> = _userSkinIndex

    // Datos temporales
    var firstName = ""
    var lastName = ""
    var birthDate = ""
    var username = ""
    var password = ""
    var email = ""

    val userEmail = MutableLiveData<String>()
    val userUUID = MutableLiveData<String>()

    fun setSkin(index: Int) {
        _userSkinIndex.value = index
    }

    fun clearNavigationToVerify() {
        _navigateToVerify.value = ""
    }

    fun startRegistration() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = ""
            try {
                val themeName = when (_userSkinIndex.value) {
                    1 -> "red"
                    2 -> "dark"
                    3 -> "teal"
                    else -> "purple"
                }

                val newUser = UserEntity(
                    uuid= "",
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate,
                    username = username,
                    email = email,
                    password = password,
                    themeColor = themeName
                )

                val result = repository.registerUser(newUser)

                if (result.success) {
                    userUUID.value = result.uuid ?: ""
                    userEmail.value = email
                    _navigateToVerify.value = email
                } else {
                    _error.value = result.message ?: "Error al registrar"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error de conexión"
            } finally {
                _loading.value = false
            }
        }
    }

    fun verifyCode(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val uuid = userUUID.value ?: ""
            if (uuid.isNotEmpty()) {
                val result = repository.verifyOtp(uuid, code)
                onResult(result.first, result.second)
            } else {
                onResult(false, "No se encontró el ID de usuario")
            }
        }
    }

    fun resendOtp(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (email.isNotEmpty()) {
                val result = repository.resendOtpByEmail(email)
                onResult(result.first, result.second)
            } else {
                onResult(false, "No se encontró el correo")
            }
        }
    }

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}