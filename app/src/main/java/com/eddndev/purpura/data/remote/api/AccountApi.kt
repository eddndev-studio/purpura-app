package com.eddndev.purpura.data.remote.api

import retrofit2.Response
import retrofit2.http.DELETE

// Servicio Retrofit de /api/v1/account (cuenta del usuario autenticado). El DELETE elimina la
// cuenta y, por cascada en el backend, todos sus datos. Lleva Bearer: el AuthInterceptor NO
// excluye esta ruta (solo excluye /auth/* y /health). Devuelve Response<Unit> para inspeccionar
// el codigo (204 esperado) sin que Retrofit lance por un 2xx sin cuerpo.
interface AccountApi {

    @DELETE("account")
    suspend fun deleteAccount(): Response<Unit>
}
