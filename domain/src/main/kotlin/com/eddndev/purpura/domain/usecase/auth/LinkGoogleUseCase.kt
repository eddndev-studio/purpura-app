package com.eddndev.purpura.domain.usecase.auth

import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import javax.inject.Inject

// Vincula una identidad de Google a la cuenta del usuario ya autenticado. Es seguro sin verificar
// el correo: la seguridad la dan la sesion vigente (probo la contrasena) y el OAuth de Google que
// el ViewModel resuelve para obtener el idToken. Tras vincular en el backend, refresca el usuario
// cacheado (conserva el token) para que la pantalla pase a mostrar Desvincular sin re-login.
class LinkGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(idToken: String): User {
        val user = authRepository.linkGoogle(idToken)
        sessionRepository.updateUser(user)
        return user
    }
}
