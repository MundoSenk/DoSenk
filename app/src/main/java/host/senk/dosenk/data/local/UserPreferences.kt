package host.senk.dosenk.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// 1. Creamos la extensión para tener una sola instancia de DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dosenk_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 2. Definimos las LLAVES (Como nombres de columnas en una tabla)
    companion object {
        val THEME_INDEX = intPreferencesKey("theme_index") // 0, 1, 2, 3
        val USER_TOKEN = stringPreferencesKey("user_token") // "abc-123-uuid"
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in") // true/false
        val USER_NAME_ALIAS = stringPreferencesKey("user_alias") // "@User"
    }

    // --- GUARDAR DATOS (Escritura) ---

    suspend fun saveTheme(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[THEME_INDEX] = index
        }
    }

    suspend fun saveUserSession(token: String, alias: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_TOKEN] = token
            preferences[USER_NAME_ALIAS] = alias
            preferences[IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    // --- LEER DATOS (Lectura reactiva con Flow) ---
    // Flow es como un río de datos: si el dato cambia, la UI se entera sola.

    val themeIndex: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[THEME_INDEX] ?: 0 // Si no hay nada, devuelve 0 (Morado)
        }

    val isUserLoggedIn: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_LOGGED_IN] ?: false
        }

    val userAlias: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[USER_NAME_ALIAS] ?: "@User"
        }
}