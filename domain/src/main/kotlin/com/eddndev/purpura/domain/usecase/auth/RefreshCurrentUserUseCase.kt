package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Refresca el usuario autenticado desde el backend (GET /auth/me) y actualiza la sesion cacheada
// conservando el token vigente. Lo dispara Inicio al volver a primer plano: si el usuario confirmo
// su correo en el navegador, emailVerified cambia a true y el aviso de "verifica tu correo"
// desaparece sin re-loguear. Mismo patron que Link/UnlinkGoogle: el caso de uso, no el repositorio,
// toca la sesion.
class RefreshCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): User {
        val user = authRepository.me()
        sessionRepository.updateUser(user)
        return user
    }
}
