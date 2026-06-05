package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// Observa la sesion para gobernar el destino inicial de navegacion: null lleva a
// AuthFragment; una sesion valida lleva a HomeFragment (06-app-architecture §8.1).
class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Session?> = sessionRepository.observeSession()
}
