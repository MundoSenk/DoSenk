package host.senk.dosenk.data.remote

import host.senk.dosenk.data.remote.model.ApiResponse
import host.senk.dosenk.data.remote.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {



    // La ruta relativa (el Base URL se pone en el Módulo)
    // Coincide con tu carpeta en Hostinger: api/v1/auth/register.php
    @POST("auth/register.php")
    suspend fun registerUser(@Body request: RegisterRequest): Response<ApiResponse>


}