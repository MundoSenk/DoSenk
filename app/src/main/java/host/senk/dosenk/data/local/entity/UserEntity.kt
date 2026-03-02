package host.senk.dosenk.data.local.entity

import android.content.IntentSender
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // Datos Personales
    val firstName: String,
    val lastName: String,
    val birthDate: String, // DD/MM/AAAA

    // Datos de Cuenta
    val username: String,
    val email: String,
    val password: String, // En local, por ahora texto plano.

    // Configuración
    val themeColor: String = "purple", // Por defecto

    // Rangos
    val rankName: String= "Desconocido",
    val dailyWastedHours: Float? = 0f,
    val setupFinished: Int? = 0
)