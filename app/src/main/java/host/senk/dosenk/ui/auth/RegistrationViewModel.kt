package host.senk.dosenk.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.repository.AuthRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val repository: AuthRepository // ¡Aquí está la conexión al Server/BD!
) : ViewModel() {

    // Skin visual
    private val _userSkinIndex = MutableLiveData<Int>(0)
    val userSkinIndex: LiveData<Int> = _userSkinIndex

    // --- MOCHILA DE DATOS (Se llena paso a paso) ---
    var firstName = ""
    var lastName = ""
    var birthDate = ""

    var username = ""
    var password = ""
    var email = ""

    fun setSkin(index: Int) {
        _userSkinIndex.value = index
    }


    fun validateAccountAvailability(user: String, mail: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            // Llamamos al repositorio (que llama a PHP)
            val result = repository.checkAvailability(user, mail)

            val isAvailable = result.first
            val message = result.second

            if (isAvailable) {
                // Si está libre, guardamos en la mochila
                username = user
                email = mail
            }

            // Avisamos a la vista (Fragment)
            onResult(isAvailable, message)
        }
    }



    // Esta función se llama desde RegisterReadyFragment
    fun registerUser(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                //  Traducir el índice del skin a texto para la BD
                val themeName = when(_userSkinIndex.value) {
                    1 -> "red"
                    2 -> "dark"
                    3 -> "teal"
                    else -> "purple"
                }

                //  Empaquetar to do en la Entidad
                val newUser = UserEntity(
                    firstName = firstName,
                    lastName = lastName,
                    birthDate = birthDate,
                    username = username,
                    email = email,
                    password = password,
                    themeColor = themeName
                )

                // Mandar al Repositorio (PHP -> Room)
                repository.registerUser(newUser)

                //  Si no tronó, es éxito
                onSuccess()

            } catch (e: Exception) {
                // Si tronó (ej: "Usuario ya existe" o "Sin internet")
                onError(e.message ?: "Error desconocido al registrar")
            }
        }
    }
}