package host.senk.dosenk.data.remote.model
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val uuid: String? = null, // poor si falla jajaja
    val username: String? = null,
    @SerializedName("theme_color")
    val themeColor: String? = null,
    @SerializedName("setup_finished")
    val setupFinished: Boolean = false // Por defecto false

)