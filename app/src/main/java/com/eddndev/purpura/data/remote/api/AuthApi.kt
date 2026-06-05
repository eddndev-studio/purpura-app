package com.eddndev.purpura.data.remote.api

import com.eddndev.purpura.data.remote.dto.AuthResponseDto
import com.eddndev.purpura.data.remote.dto.GoogleAuthRequest
import com.eddndev.purpura.data.remote.dto.LoginRequest
import com.eddndev.purpura.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

// Servicio Retrofit de /api/v1/auth/* (contrato §5.2..§5.4). Estas rutas NO llevan Bearer
// (el AuthInterceptor las excluye).
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @POST("auth/google")
    suspend fun google(@Body body: GoogleAuthRequest): AuthResponseDto
}
