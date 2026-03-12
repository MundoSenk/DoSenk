package host.senk.dosenk.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "block_profiles")
data class BlockProfileEntity(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val userUuid: String,
    val name: String,
    val blockedAppsJson: String
)