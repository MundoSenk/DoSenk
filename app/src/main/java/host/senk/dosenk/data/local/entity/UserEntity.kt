package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(

    @PrimaryKey val uuid: String,

    // Datos Personales
    val firstName: String,
    val lastName: String,
    val birthDate: String, // DD/MM/AAAA

    // Datos de Cuenta
    val username: String,
    val email: String,
    val password: String,

    // Configuración
    val themeColor: String = "purple",

    // Rangos
    val rankName: String = "Desconocido",
    val dailyWastedHours: Float? = 0f,
    val setupFinished: Int? = 0
)