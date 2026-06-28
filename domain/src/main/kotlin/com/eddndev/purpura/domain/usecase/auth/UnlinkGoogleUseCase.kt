package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Quita la identidad de Google de la cuenta autenticada. El backend rechaza la desvinculacion si
// dejaria a la cuenta sin forma de entrar (sin contrasena) -> CannotUnlinkGoogle, que el ViewModel
// traduce a un aviso. En exito refresca el usuario cacheado (conserva el token) para que la
// pantalla vuelva a mostrar Vincular.
class UnlinkGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): User {
        val user = authRepository.unlinkGoogle()
        sessionRepository.updateUser(user)
        return user
    }
}
