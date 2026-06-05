package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Registro de cuenta local (contrato §5.2). Persiste la sesion al exito; email duplicado se
// mapea a DomainError.EmailTaken en la capa data.
class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(email: String, nombre: String, password: String): AuthResult {
        val result = authRepository.register(email.trim(), nombre.trim(), password)
        sessionRepository.persist(result)
        return result
    }
}
