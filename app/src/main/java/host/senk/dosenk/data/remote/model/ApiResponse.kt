package host.senk.dosenk.data.remote.model

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val uuid: String? = null // poor si falla jajaja

)