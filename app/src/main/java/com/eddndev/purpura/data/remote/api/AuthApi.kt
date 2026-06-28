package com.eddndev.purpura.data.remote.api

import com.eddndev.purpura.data.remote.dto.AuthResponseDto
import com.eddndev.purpura.data.remote.dto.GoogleAuthRequest
import com.eddndev.purpura.data.remote.dto.LoginRequest
import com.eddndev.purpura.data.remote.dto.RegisterRequest
import com.eddndev.purpura.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Servicio Retrofit de /api/v1/auth/* (contrato §5.2..§5.4 + verificacion de correo).
// register/login/google son PUBLICAS (sin Bearer; el AuthInterceptor las excluye por path).
// me y verify-email/request requieren Bearer: el interceptor SOLO excluye las publicas, asi que a
// estas les adjunta el token igual que a /account.
interface AuthApi {

    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponseDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponseDto

    @POST("auth/google")
    suspend fun google(@Body body: GoogleAuthRequest): AuthResponseDto

    // Usuario autenticado fresco (200 con cuerpo). Refresca emailVerified/nombre sin re-loguear.
    @GET("auth/me")
    suspend fun me(): UserDto

    // Pide el correo de verificacion (202 sin cuerpo). Response<Unit> para inspeccionar el codigo sin
    // que Retrofit lance por un 2xx sin cuerpo (mismo patron que AccountApi.deleteAccount).
    @POST("auth/verify-email/request")
    suspend fun requestEmailVerification(): Response<Unit>
}
