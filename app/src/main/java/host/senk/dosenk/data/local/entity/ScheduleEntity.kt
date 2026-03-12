package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index // 🚨 Importa esto
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "schedules",
    indices = [Index(value = ["userUuid"])],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["uuid"],
            childColumns = ["userUuid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduleEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val userUuid: String,
    val type: String,
    val gridJson: String
)