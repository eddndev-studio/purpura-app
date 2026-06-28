package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.repository.AuthRepository
import javax.inject.Inject

// Pide al backend que envie un correo de verificacion al usuario autenticado
// (POST /auth/verify-email/request). No toca la sesion: el correo solo queda verificado cuando el
// usuario abre el enlace; el cambio se refleja luego via RefreshCurrentUserUseCase. Idempotente en
// el backend (si ya esta verificado, no envia nada).
class RequestEmailVerificationUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.requestEmailVerification()
}
