package host.senk.dosenk.data.repository

import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.remote.ApiService
import host.senk.dosenk.data.remote.model.ApiResponse
import host.senk.dosenk.data.remote.model.CheckRequest
import host.senk.dosenk.data.remote.model.LoginRequest
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.ScheduleBatchRequest
import host.senk.dosenk.data.remote.model.ScheduleData
import host.senk.dosenk.data.remote.model.SaveVicesRequest
import host.senk.dosenk.data.remote.model.ViceDto
import host.senk.dosenk.util.AppUsageInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) {

    /**
     * Registro de usuario: Envía datos al servidor, y si es exitoso,
     * guarda al usuario en la base de datos local (Room) y en preferencias.
     */
    suspend fun registerUser(user: UserEntity): ApiResponse {
        val request = RegisterRequest(
            username = user.username,
            email = user.email,
            password = user.password,
            firstName = user.firstName,
            lastName = user.lastName,
            birthDate = user.birthDate,
            themeColor = user.themeColor
        )

        val response = api.registerUser(request)

        if (response.isSuccessful && response.body() != null) {
            val serverResponse = response.body()!!

            if (serverResponse.success) {
                // Guardar localmente solo si el servidor aceptó
                userDao.insertUser(user)

                userPreferences.saveUserSession(
                    token = serverResponse.uuid ?: "no-uuid",
                    alias = user.username
                )

                val themeIndex = when (user.themeColor) {
                    "red" -> 1
                    "dark" -> 2
                    "teal" -> 3
                    else -> 0
                }
                userPreferences.saveTheme(themeIndex)
            }
            return serverResponse
        } else {
            throw Exception("Error del servidor: ${response.code()}")
        }
    }

    /**
     * Verifica si el nombre de usuario o email ya están registrados.
     */
    suspend fun checkAvailability(username: String, email: String): Pair<Boolean, String> {
        return try {
            val request = CheckRequest(username, email)
            val response = api.checkAvailability(request)

            if (response.isSuccessful && response.body() != null) {
                val apiResponse = response.body()!!
                Pair(apiResponse.success, apiResponse.message)
            } else {
                Pair(false, "Error del servidor: ${response.code()}")
            }
        } catch (e: Exception) {
            Pair(false, "Error de conexión: ${e.message}")
        }
    }

    /**
     * Inicio de sesión: Intenta primero vía API. Si no hay conexión,
     * intenta validar contra la base de datos local (Modo Offline).
     */
    suspend fun login(userOrEmail: String, pass: String): Pair<Boolean, String> {
        return try {
            val request = LoginRequest(userOrEmail, pass)
            val response = api.loginUser(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!

                userPreferences.saveUserSession(
                    token = data.uuid ?: "uuid-error",
                    alias = data.username ?: userOrEmail
                )

                val themeIndex = when (data.themeColor) {
                    "red" -> 1
                    "dark" -> 2
                    "teal" -> 3
                    else -> 0
                }
                userPreferences.saveTheme(themeIndex)
                userPreferences.saveSetupFinished(data.setupFinished)

                Pair(true, "Bienvenido")
            } else {
                val msg = response.body()?.message ?: "Error de credenciales"
                Pair(false, msg)
            }
        } catch (e: Exception) {
            // LOGIN OFFLINE
            val localUser = userDao.getUserByEmailOrUsername(userOrEmail)
            if (localUser != null && localUser.password == pass) {
                userPreferences.saveUserSession("offline_token", localUser.username)
                Pair(true, "Bienvenido (Modo Offline)")
            } else {
                Pair(false, "Sin conexión y credenciales no guardadas")
            }
        }
    }

    /**
     * Guarda la lista de horarios/actividades del usuario.
     */
    suspend fun saveSchedules(schedules: List<ScheduleData>): Boolean {
        return try {
            val uuid = userPreferences.userToken.first()
            if (uuid.isEmpty()) return false

            val request = ScheduleBatchRequest(uuid, schedules)
            val response = api.saveSchedules(request)

            if (response.isSuccessful && response.body()?.success == true) {
                userPreferences.saveSetupFinished(1)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Guarda el nivel de disciplina y las aplicaciones detectadas como "vicios".
     */
    suspend fun saveDisciplineLevel(
        dailyHours: Float,
        rankName: String,
        vices: List<AppUsageInfo>
    ): Boolean {
        return try {
            val uuid = userPreferences.userToken.first()
            val usernameAlias = userPreferences.userAlias.first()

            if (uuid.isEmpty()) return false

            val vicesDtoList = vices.map { app ->
                ViceDto(
                    package_name = app.packageName,
                    app_name = app.appName,
                    time_spent_ms = app.timeInForegroundMillis
                )
            }

            val request = SaveVicesRequest(
                uuid = uuid,
                daily_wasted_hours = dailyHours,
                rank_name = rankName,
                vices = vicesDtoList
            )

            val response = api.saveVicesAndRank(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val currentUser = userDao.getUserByEmailOrUsername(usernameAlias)
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(
                        rankName = rankName,
                        dailyWastedHours = dailyHours,
                        setupFinished = 2
                    )
                    userDao.updateUser(updatedUser)
                }
                userPreferences.saveSetupFinished(2)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    //  (Recibe UUID y OTP)
    suspend fun verifyOtp(uuid: String, code: String): Pair<Boolean, String> {
        return try {
            val response = api.verifyOtp(mapOf("uuid" to uuid, "otp" to code))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Pair(body.success, body.message ?: "Verificación exitosa")
            } else {
                Pair(false, "Código incorrecto o expirado")
            }
        } catch (e: Exception) {
            Pair(false, "Error de conexión")
        }
    }

    // (Recibe Email)
    suspend fun resendOtpByEmail(email: String): Pair<Boolean, String> {
        return try {
            val response = api.resendCode(mapOf("email" to email))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                Pair(body.success, body.message ?: "Código enviado")
            } else {
                Pair(false, "Error al reenviar")
            }
        } catch (e: Exception) {
            Pair(false, "Error de red")
        }
    }
}
