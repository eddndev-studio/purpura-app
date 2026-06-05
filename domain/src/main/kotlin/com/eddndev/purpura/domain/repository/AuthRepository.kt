package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.AuthResult

// Puerto del dominio: espejo de /api/v1/auth/* (contrato §5.2..§5.4). Tras un resultado
// exitoso, la capa data persiste la sesion via SessionRepository.
interface AuthRepository {
    suspend fun register(email: String, nombre: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun loginWithGoogle(idToken: String): AuthResult
}
