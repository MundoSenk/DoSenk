package host.senk.dosenk.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class RegistrationViewModel : ViewModel() {

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
}