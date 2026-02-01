package host.senk.dosenk.data.remote.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    //los @SerializeName deben ser identicos al php
    @SerializedName("first_name")
    val firstName: String,

    @SerializedName("last_name")
    val lastName: String,

    @SerializedName("birth_date")
    val birthDate: String,

    @SerializedName("theme_color")
    val themeColor: String
)