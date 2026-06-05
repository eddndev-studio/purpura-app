package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Login con correo y contrasena (contrato §5.3). Persiste la sesion al exito; un fallo de
// credenciales se mapea a DomainError.InvalidCredential en la capa data.
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(email: String, password: String): AuthResult {
        val result = authRepository.login(email.trim(), password)
        sessionRepository.persist(result)
        return result
    }
}
