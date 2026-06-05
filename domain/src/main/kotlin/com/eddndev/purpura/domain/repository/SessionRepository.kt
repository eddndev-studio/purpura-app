package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import kotlinx.coroutines.flow.Flow

// Puerto de sesion local (06-app-architecture §4.2). Envuelve el almacenamiento seguro del
// JWT y el usuario cacheado. `null` = no autenticado.
interface SessionRepository {
    fun observeSession(): Flow<Session?>
    suspend fun persist(result: AuthResult)
    suspend fun currentToken(): String?   // lo consume el AuthInterceptor
    suspend fun clear()                   // cierre de sesion: borra token y cache
}
