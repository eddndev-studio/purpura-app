package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Elimina la cuenta de forma permanente. Orden deliberado: primero borra en el backend
// (DELETE /account, requiere el token vigente) y SOLO si tuvo exito limpia la sesion local
// (token + cache). Si el borrado remoto falla, la sesion se conserva y el error se propaga
// al ViewModel para mostrarlo. Tras clear(), MainActivity.observeSessionGate detecta la
// sesion null y navega a Auth, igual que LogoutUseCase. No es reversible.
//
// Idempotencia: el backend responde 404 user_not_found si la cuenta ya no existe (un 204
// perdido por la red y un reintento, o un borrado desde otro dispositivo). Ese caso YA cumple
// el estado deseado -- la cuenta no existe -- asi que lo tratamos como exito y limpiamos la
// sesion igualmente, en vez de dejar al usuario atrapado con una sesion fantasma y un aviso.
class DeleteAccountUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() {
        try {
            authRepository.deleteAccount()
        } catch (alreadyGone: DomainError.UserNotFound) {
            // La cuenta ya no existe en el backend: el borrado es idempotente, seguimos a clear().
        }
        sessionRepository.clear()
    }
}
