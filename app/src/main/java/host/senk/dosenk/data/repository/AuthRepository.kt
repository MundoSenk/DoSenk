package host.senk.dosenk.data.repository

import host.senk.dosenk.data.local.UserPreferences
import host.senk.dosenk.data.local.dao.UserDao
import host.senk.dosenk.data.local.entity.UserEntity
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val userDao: UserDao,
    private val userPreferences: UserPreferences
) {

    // --- LOGICA DE REGISTRO ---
    suspend fun registerUser(user: UserEntity) {
        // Guardar en BD Local
        userDao.insertUser(user)

        // Iniciar sesión automáticamente (Guardar en Prefs)
        userPreferences.saveUserSession(
            token = "dummy_token_${user.id}", // Luego vendrá del servidor real
            alias = user.username
        )

        // Guardar su tema preferido
        val themeIndex = when(user.themeColor) {
            "red" -> 1
            "dark" -> 2
            "teal" -> 3
            else -> 0 // Purple
        }
        userPreferences.saveTheme(themeIndex)
    }


    suspend fun login(email: String, pass: String): Boolean {
        //  Buscar usuario por email
        val user = userDao.getUserByEmail(email) ?: return false // No existe

        //  Checar password
        if (user.passwordHash == pass) {

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