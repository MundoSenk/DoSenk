package host.senk.dosenk.data.remote

import host.senk.dosenk.data.remote.model.ApiResponse
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.CheckRequest
import host.senk.dosenk.data.remote.model.LoginRequest
import host.senk.dosenk.data.remote.model.ScheduleBatchRequest
import host.senk.dosenk.data.remote.model.SaveVicesRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("auth/register.php")
    suspend fun registerUser(@Body request: RegisterRequest): Response<ApiResponse>

    @POST("auth/check_availability.php")
    suspend fun checkAvailability(@Body request: CheckRequest): Response<ApiResponse>

    @POST("auth/login.php")
    suspend fun loginUser(@Body request: LoginRequest): Response<ApiResponse>

    @POST("schedule/save_batch.php")
    suspend fun saveSchedules(@Body request: ScheduleBatchRequest): Response<ApiResponse>

    @POST("user/save_vices.php")
    suspend fun saveVicesAndRank(@Body request: SaveVicesRequest): Response<ApiResponse>

    @POST("auth/verify_otp.php")
    suspend fun verifyOtp(@Body body: Map<String, String>): Response<ApiResponse>

    @POST("auth/resend_otp.php")
    suspend fun resendCode(@Body body: Map<String, String>): Response<ApiResponse>
}