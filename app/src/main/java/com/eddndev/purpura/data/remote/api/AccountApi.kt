package com.eddndev.purpura.data.remote.api

import com.eddndev.purpura.data.remote.dto.GoogleAuthRequest
import com.eddndev.purpura.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

// Servicio Retrofit de /api/v1/account (cuenta del usuario autenticado). Todas las rutas llevan
// Bearer: el AuthInterceptor NO excluye /account (solo excluye /auth/* y /health).
interface AccountApi {

    // Elimina la cuenta y, por cascada en el backend, todos sus datos. Response<Unit> para
    // inspeccionar el codigo (204 esperado) sin que Retrofit lance por un 2xx sin cuerpo.
    @DELETE("account")
    suspend fun deleteAccount(): Response<Unit>

    // Vincula la identidad de Google del idToken a la cuenta autenticada. Devuelve el usuario
    // actualizado (200, googleLinked=true). El backend toma al usuario de la sesion, no del cuerpo.
    @POST("account/link-google")
    suspend fun linkGoogle(@Body body: GoogleAuthRequest): UserDto

    // Desvincula Google de la cuenta autenticada. Devuelve el usuario actualizado (200,
    // googleLinked=false). A diferencia de deleteAccount, responde 200 con cuerpo, no 204.
    @DELETE("account/link-google")
    suspend fun unlinkGoogle(): UserDto
}
