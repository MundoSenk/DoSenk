package host.senk.dosenk.data.repository

import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.dao.BlockProfileDao
import host.senk.dosenk.data.local.entity.UserEntity
import host.senk.dosenk.data.local.entity.ScheduleEntity
import host.senk.dosenk.data.local.entity.BlockProfileEntity
import host.senk.dosenk.data.remote.ApiService
import host.senk.dosenk.data.remote.model.ApiResponse
import host.senk.dosenk.data.remote.model.CheckRequest
import host.senk.dosenk.data.remote.model.LoginRequest
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.ScheduleBatchRequest
import host.senk.dosenk.data.remote.model.ScheduleData
import host.senk.dosenk.data.remote.model.SaveVicesRequest
import host.senk.dosenk.data.remote.model.UpdateStageRequest
import host.senk.dosenk.data.remote.model.ViceDto
import host.senk.dosenk.util.AppUsageInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val api: ApiService,
    private val userDao: UserDao,
    private val blockProfileDao: BlockProfileDao,
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
                //  Extraemos el UUID real que nos dio PHP
                val realUuid = serverResponse.uuid ?: "no-uuid"

                val finalUser = user.copy(uuid = realUuid)
                userDao.insertUser(finalUser)

                userPreferences.saveUserSession(
                    token = realUuid,
                    alias = finalUser.username
                )

                val themeIndex = when (finalUser.themeColor) {
                    "red" -> 1; "dark" -> 2; "teal" -> 3; else -> 0
                }
                userPreferences.saveTheme(themeIndex)
            }
            return serverResponse
        } else {
            throw Exception("Error del servidor: ${response.code()}")
        }
    }

    /**
     * Inicio de sesión: Destruye la base de datos local vieja y
     * reconstruye "Todo Todito" con los datos del servidor.
     */
    suspend fun login(userOrEmail: String, pass: String): Pair<Boolean, String> {
        return try {
            val request = LoginRequest(username = userOrEmail, password = pass)
            val response = api.loginUser(request)

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!
                val userUuid = data.uuid ?: "uuid-error"
                val alias = data.username ?: userOrEmail

                //LIMPIEZA TOTAL DE LA BASE DE DATOS LOCAL
                userDao.deleteAllUsers()
                userDao.deleteAllSchedules()
                blockProfileDao.deleteAllProfiles()

                // RECONSTRUIMOS EL USUARIO
                val loggedInUser = UserEntity(
                    uuid = userUuid,
                    username = alias,
                    email = data.email ?: userOrEmail,
                    password = pass, // Lo mantenemos local para el modo offline
                    firstName = data.firstName ?: "Desconocido",
                    lastName = data.lastName ?: "Desconocido",
                    birthDate = data.birthDate ?: "0000-00-00",
                    themeColor = data.themeColor ?: "purple",
                    setupFinished = data.setupFinished ?: 0,
                    dailyWastedHours = data.dailyWastedHours ?: 0f,
                    rankName = data.rankName ?: "Desconocido",
                    currentXp = data.currentXp ?: 0,
                    streakDays = data.streakDays ?: 1
                )
                userDao.insertUser(loggedInUser)

                // RECONSTRUIMOS SUS HORARIOS
                data.schedules?.forEach { sched ->
                    userDao.insertSchedule(
                        ScheduleEntity(
                            userUuid = userUuid,
                            type = sched.type,
                            gridJson = sched.gridJson
                        )
                    )
                }

                // RECONSTRUIMOS SUS BLOQUEOS PERSONALIZADOS
                data.blockProfiles?.forEach { block ->
                    blockProfileDao.insertProfile(
                        BlockProfileEntity(
                            userUuid = userUuid,
                            name = block.name,
                            blockedAppsJson = block.blockedAppsJson
                        )
                    )
                }

                // GUARDAMOS EN PREFERENCIAS
                userPreferences.saveUserSession(token = userUuid, alias = alias)

                val themeIndex = when (loggedInUser.themeColor) {
                    "red" -> 1; "dark" -> 2; "teal" -> 3; else -> 0
                }
                userPreferences.saveTheme(themeIndex)
                userPreferences.saveSetupFinished(loggedInUser.setupFinished ?: 0)


                if (!data.createdAt.isNullOrEmpty()) {
                    try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        val serverDate = sdf.parse(data.createdAt)
                        if (serverDate != null) {
                            userPreferences.saveStartDate(serverDate.time)
                        }
                    } catch (e: Exception) { e.printStackTrace() }
                }




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

                    // LEEMOS LA XP DEL RANGO DIRECTO DE TU CLASE LOCAL
                    val startingXp = host.senk.dosenk.util.DoRank.entries.find { it.title == rankName }?.threshold ?: 0

                    // LE INYECTAMOS LA XP AL USUARIO LOCAL
                    val updatedUser = currentUser.copy(
                        rankName = rankName,
                        dailyWastedHours = dailyHours,
                        setupFinished = 2,
                        currentXp = startingXp
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



    /**
     * Guarda la etapa del tutorial localmente y la sincroniza con la nube.
     */
    suspend fun updateSetupStage(stage: Int): Boolean {
        return try {
            val uuid = userPreferences.userToken.first()
            if (uuid.isEmpty()) return false

            //  Guardado ultra-rápido local (DataStore)
            userPreferences.saveSetupFinished(stage)

            // Disparo a la nube (MySQL)
            val request = UpdateStageRequest(uuid, stage)
            val response = api.updateSetupStage(request)

            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }



    /**
     * Respalda las misiones locales en la nube.
     */
    suspend fun syncMissionsToCloud(missions: List<host.senk.dosenk.data.local.entity.MissionEntity>): Boolean {
        return try {
            val uuid = userPreferences.userToken.first()
            if (uuid.isEmpty() || missions.isEmpty()) return false

            // Transformamos las misiones locales al molde de la API
            val dtoList = missions.map {
                host.senk.dosenk.data.remote.model.MissionDto(
                    uuid = it.uuid,
                    name = it.name,
                    description = it.description,
                    durationMinutes = it.durationMinutes,
                    executionDate = it.executionDate,
                    assignmentType = it.assignmentType,
                    blockType = it.blockType,
                    status = it.status,
                    potentialXp = it.potentialXp,
                    earnedXp = it.earnedXp,
                    multiplierApplied = it.multiplierApplied
                )
            }

            // Armamos la caja
            val request = host.senk.dosenk.data.remote.model.SyncMissionsRequest(
                user_uuid = uuid,
                missions = dtoList
            )

            // ¡Fuego!
            val response = api.syncMissions(request)

            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }




    ///// MANDAR RACHA Y SXP A LA REMOTA!

    suspend fun syncStatsToCloud(xp: Int, streak: Int): Boolean {
        return try {
            val uuid = userPreferences.userToken.first()
            if (uuid.isEmpty()) return false

            val request = host.senk.dosenk.data.remote.model.SyncStatsRequest(uuid, xp, streak)
            val response = api.syncUserStats(request)
            response.isSuccessful && response.body()?.success == true
        } catch (e: Exception) { false }
    }


}