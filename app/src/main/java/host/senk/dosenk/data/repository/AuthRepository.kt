package host.senk.dosenk.data.repository

import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.remote.ApiService
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.CheckRequest
import host.senk.dosenk.data.remote.model.LoginRequest
import host.senk.dosenk.data.remote.model.ScheduleBatchRequest
import host.senk.dosenk.data.remote.model.ScheduleData
import kotlinx.coroutines.flow.first
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


    suspend fun login(userOrEmail: String, pass: String): Pair<Boolean, String> {
        try {
            //  INTENTO REMOTO
            val request = LoginRequest(userOrEmail, pass)
            val response = api.loginUser(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!

                //  Guardar sesión
                userPreferences.saveUserSession(
                    token = data.uuid ?: "uuid-error",
                    alias = data.username ?: userOrEmail
                )

                // Aplicar tema del usuario
                val themeIndex = when(data.themeColor) {
                    "red" -> 1
                    "dark" -> 2
                    "teal" -> 3
                    else -> 0
                }
                userPreferences.saveTheme(themeIndex)

                userPreferences.saveSetupFinished(data.setupFinished)

                return Pair(true, "Bienvenido")
            } else {
                // FALLÓ EN NUBE
                val msg = response.body()?.message ?: "Error de credenciales"
                return Pair(false, msg)
            }

        } catch (e: Exception) {
            // SI NO HAY INTERNET -> Intentar Login Local (Offline)
            // Esto asume que el usuario ya se había logueado/registrado en este cel antes
            val localUser = userDao.getUserByEmailOrUsername(userOrEmail)
            if (localUser != null && localUser.password == pass) {
                // Login Local Exitoso
                userPreferences.saveUserSession("offline_token", localUser.username)
                return Pair(true, "Bienvenido (Modo Offline)")
            }

            return Pair(false, "Sin conexión y credenciales no guardadas")
        }
    }


    suspend fun saveSchedules(schedules: List<ScheduleData>): Boolean {
        try {
            //  Obtener el UUID del usuario actual (Token)
            val uuid = userPreferences.userToken.first()

            if (uuid.isEmpty()) return false

            //  Armar petición
            val request = ScheduleBatchRequest(uuid, schedules)
            val response = api.saveSchedules(request)

            if (response.isSuccessful && response.body()?.success == true) {
               // Marcamos localmente que ya acabó el tutorial
                userPreferences.saveSetupFinished(true)
                return true
            }
            return false
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


}