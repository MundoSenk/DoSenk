package host.senk.dosenk.data.remote.model

import com.google.gson.annotations.SerializedName

data class ScheduleBatchRequest(
    val uuid: String, // El ID del usuario
    val schedules: List<ScheduleData>
)

data class ScheduleData(
    val type: String, // "SCHOOL", "WORK", "BUSINESS"
    val grid: String  // El JSON de la matriz convertido a String
)