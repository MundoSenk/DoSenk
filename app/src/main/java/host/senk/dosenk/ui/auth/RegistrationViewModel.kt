package host.senk.dosenk.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope // Importante para lanzar corrutinas
import dagger.hilt.android.lifecycle.HiltViewModel
import host.senk.dosenk.data.local.UserPreferences
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel //Anotación clave para Hilt
class RegistrationViewModel @Inject constructor(
    private val userPreferences: UserPreferences // Inyectamos la memoria
) : ViewModel() {

    // Aquí guardamos el Skin que eligió el usuario en el carrusel
    private val _userSkinIndex = MutableLiveData<Int>(0) // 0 por defecto
    val userSkinIndex: LiveData<Int> = _userSkinIndex

    // Aquí iremos acumulando los datos de los 4 fragmentos antes de enviarlos a la API
    // Personales
    var firstName = ""
    var lastName = ""
    var birthDate = ""
    var gender = ""

    // Cuenta
    var username = ""
    var password = ""
    var email = ""

    fun setSkin(index: Int) {
        _userSkinIndex.value = index
    }


    fun completeRegistration() {
        viewModelScope.launch {
            // Guardamos el tema seleccionado permanentemente
            val skin = _userSkinIndex.value ?: 0
            userPreferences.saveTheme(skin)

            // Simulamos guardar una sesión (luego vendrá del backend real)
            userPreferences.saveUserSession("dummy_token_123", "@${firstName.ifEmpty { "User" }}")
        }
    }
}