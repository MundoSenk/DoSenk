package host.senk.dosenk.data.repository

import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.remote.ApiService
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.CheckRequest
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,     //Conexión Nube
    private val userDao: UserDao,    // Conexión Local
    private val userPreferences: UserPreferences // Configuración
) {

    suspend fun registerUser(user: UserEntity) {

        //  Convertir Entidad Local -> Request Remoto
        val request = RegisterRequest(
            username = user.username,
            email = user.email,
            password = user.password,
            firstName = user.firstName,
            lastName = user.lastName,
            birthDate = user.birthDate,
            themeColor = user.themeColor
        )

        // MANDAR A PHP (Retrofit)
        val response = api.registerUser(request)

        //  Verificar respuesta
        if (response.isSuccessful && response.body()?.success == true) {
            val serverResponse = response.body()!!


            // Ahora sí, guardamos en Room para uso offline
            userDao.insertUser(user)

            // Guardamos sesión activa
            userPreferences.saveUserSession(
                token = serverResponse.uuid ?: "no-uuid", // Usamos el UUID real del server
                alias = user.username
            )

            // Guardar tema
            val themeIndex = when(user.themeColor) {
                "red" -> 1
                "dark" -> 2
                "teal" -> 3
                else -> 0
            }
            userPreferences.saveTheme(themeIndex)

        } else {
            // ERROR DEL SERVIDOR (Ej: "Usuario ya existe" o error 500)
            val errorMsg = response.body()?.message ?: "Error del servidor: ${response.code()}"
            throw Exception(errorMsg) // Esto lo atrapará el ViewModel y mostrará Toast
        }
    }


    suspend fun checkAvailability(username: String, email: String): Pair<Boolean, String> {
        try {
            val request = CheckRequest(username, email)
            val response = api.checkAvailability(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                // Si success es true, está libre. Si es false, mensaje del PHP.
                return Pair(apiResponse.success, apiResponse.message)
            } else {
                return Pair(false, "Error del servidor: ${response.code()}")
            }
        } catch (e: Exception) {
            return Pair(false, "Error de conexión: ${e.message}")
        }
    }


    suspend fun login(email: String, pass: String): Boolean {
        //  Buscar usuario por email
        val user = userDao.getUserByEmailOrUsername(email) ?: return false // No existe

        //  Checar password
        if (user.password == pass) {

            userPreferences.saveUserSession(
                token = "dummy_token_${user.id}",
                alias = user.username
            )
            // Restaurar su tema
            val themeIndex = when(user.themeColor) {
                "red" -> 1
                "dark" -> 2
                "teal" -> 3
                else -> 0
            }
            userPreferences.saveTheme(themeIndex)

            return true
        }

        return false // Password incorrecto
    }
}