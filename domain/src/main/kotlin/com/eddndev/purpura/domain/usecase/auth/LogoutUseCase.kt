package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Cierre de sesion: borra el token y el cache local para no filtrar datos entre cuentas
// (06-app-architecture §9, regla 6). No es REQ-EXIT (eso finaliza la app).
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() = sessionRepository.clear()
}
