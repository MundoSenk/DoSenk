package host.senk.dosenk.data.remote.model
import com.google.gson.annotations.SerializedName


data class ApiResponse(
    val success: Boolean,
    val message: String?,
    val uuid: String?,
    val username: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val birthDate: String?,
    val themeColor: String?,
    val setupFinished: Int?
)