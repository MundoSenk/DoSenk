package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "schedules",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE // Si borras al usuario, se borran sus horarios
        )
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val type: String, // "SCHOOL", "WORK", "BUSINESS"
    val gridJson: String // El array de 7x24 convertido a String
)