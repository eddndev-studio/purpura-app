package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.User

// Puerto del dominio: espejo de /api/v1/auth/* (contrato §5.2..§5.4). Tras un resultado
// exitoso, la capa data persiste la sesion via SessionRepository.
interface AuthRepository {
    suspend fun register(email: String, nombre: String, password: String): AuthResult
    suspend fun login(email: String, password: String): AuthResult
    suspend fun loginWithGoogle(idToken: String): AuthResult

    // Elimina la cuenta del usuario autenticado en el backend (DELETE /account); por cascada
    // el servidor borra tambien todos sus datos. La limpieza de la sesion local la hace el
    // caso de uso (DeleteAccountUseCase) via SessionRepository, no este repositorio.
    suspend fun deleteAccount()

    // Adjunta la identidad de Google del idToken a la cuenta autenticada (POST /account/link-google)
    // y devuelve el usuario actualizado (googleLinked=true). El backend identifica al usuario por su
    // sesion, no por el cuerpo. Conflicto (ese Google ya esta en otra cuenta) -> GoogleLinkConflict.
    suspend fun linkGoogle(idToken: String): User

    // Quita la identidad de Google de la cuenta autenticada (DELETE /account/link-google) y devuelve
    // el usuario actualizado (googleLinked=false). Si la cuenta no tiene contrasena (quedaria sin
    // forma de entrar) -> CannotUnlinkGoogle.
    suspend fun unlinkGoogle(): User

    // Devuelve el usuario autenticado fresco del backend (GET /auth/me). Sirve para refrescar
    // emailVerified/nombre tras confirmar el correo en el navegador, sin re-loguear. El refresco de
    // la sesion local (conservando el token) lo hace el caso de uso via SessionRepository.
    suspend fun me(): User

    // Pide al backend que envie un correo de verificacion al usuario autenticado
    // (POST /auth/verify-email/request). Idempotente: si ya esta verificado, el backend no envia nada
    // (202 igual). No cambia la sesion.
    suspend fun requestEmailVerification()
}
