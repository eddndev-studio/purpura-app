package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Elimina la cuenta de forma permanente. Orden deliberado: primero borra en el backend
// (DELETE /account, requiere el token vigente) y SOLO si tuvo exito limpia la sesion local
// (token + cache). Si el borrado remoto falla, la sesion se conserva y el error se propaga
// al ViewModel para mostrarlo. Tras clear(), MainActivity.observeSessionGate detecta la
// sesion null y navega a Auth, igual que LogoutUsecase. No es reversible.
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        authRepository.deleteAccount()
        sessionRepository.clear()
    }
}
