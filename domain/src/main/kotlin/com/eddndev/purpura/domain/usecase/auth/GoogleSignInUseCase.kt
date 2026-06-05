package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Intercambio de idToken de Google por el JWT propio de Purpura (contrato §5.4). Persiste la
// sesion al exito.
class GoogleSignInUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(idToken: String): AuthResult {
        val result = authRepository.loginWithGoogle(idToken)
        sessionRepository.persist(result)
        return result
    }
}
