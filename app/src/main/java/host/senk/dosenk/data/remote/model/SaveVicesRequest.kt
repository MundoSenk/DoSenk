package host.senk.dosenk.data.remote.model

// el modelo para el php
data class SaveVicesRequest(
    val uuid: String,
    val daily_wasted_hours: Float,
    val rank_name: String,
    val vices: List<ViceDto>
)

// como se structura cada app
data class ViceDto(
    val package_name: String,
    val app_name: String,
    val time_spent_ms: Long
)