package host.senk.dosenk.data.remote

import host.senk.dosenk.data.remote.model.ApiResponse
import host.senk.dosenk.data.remote.model.RegisterRequest
import host.senk.dosenk.data.remote.model.CheckRequest
import host.senk.dosenk.data.remote.model.LoginRequest
import host.senk.dosenk.data.remote.model.ScheduleBatchRequest
import host.senk.dosenk.data.remote.model.SaveVicesRequest
import host.senk.dosenk.data.remote.model.SyncBlocksRequest
import host.senk.dosenk.data.remote.model.UpdateStageRequest
import host.senk.dosenk.data.remote.model.SyncMissionsRequest
import host.senk.dosenk.data.remote.model.SyncStatsRequest
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


    //Aqui se subo el setup a 2, cambiado a 3
    @POST("user/save_vices.php")
    suspend fun saveVicesAndRank(@Body request: SaveVicesRequest): Response<ApiResponse>

    @POST("auth/verify_otp.php")
    suspend fun verifyOtp(@Body body: Map<String, String>): Response<ApiResponse>

    @POST("auth/resend_otp.php")
    suspend fun resendCode(@Body body: Map<String, String>): Response<ApiResponse>


    @POST("auth/update_stage.php")
    suspend fun updateSetupStage(@Body request: UpdateStageRequest): Response<ApiResponse>

    @POST("user/sync_blocks.php")
    suspend fun syncBlocks(@Body request: SyncBlocksRequest): Response<ApiResponse>

    // Sincronizar las misiones completadas/pendientes a la nube
    @POST("user/sync_missions.php")
    suspend fun syncMissions(@Body request: SyncMissionsRequest): Response<ApiResponse>


    @POST("user/sync_user_stats.php")
    suspend fun syncUserStats(@Body request: SyncStatsRequest): Response<ApiResponse>

}