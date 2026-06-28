package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.model.User
import kotlinx.coroutines.flow.Flow

// Puerto de sesion local (06-app-architecture §4.2). Envuelve el almacenamiento seguro del
// JWT y el usuario cacheado. `null` = no autenticado.
interface SessionRepository {
    fun observeSession(): Flow<Session?>
    suspend fun persist(result: AuthResult)
    // Refresca SOLO el usuario cacheado conservando el token vigente (vincular/desvincular Google
    // devuelven el usuario actualizado, no un token nuevo). No-op si no hay sesion.
    suspend fun updateUser(user: User)
    suspend fun currentToken(): String?   // lo consume el AuthInterceptor
    suspend fun clear()                   // cierre de sesion: borra token y cache
    // Invalidacion SINCRONA por token expirado/invalido (401 en endpoint protegido). Borra el token
    // y emite sesion null para que el gate de navegacion lleve a Auth. La llama el AuthInterceptor
    // desde el hilo de OkHttp, por eso no es suspend ni toca Room (solo estado de sesion + token).
    fun invalidate()
}
