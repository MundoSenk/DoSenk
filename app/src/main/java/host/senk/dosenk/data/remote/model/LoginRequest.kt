package host.senk.dosenk.data.remote.model

data class LoginRequest(
    val username: String, // Enviamos lo que escriba el usuario
    val password: String
)