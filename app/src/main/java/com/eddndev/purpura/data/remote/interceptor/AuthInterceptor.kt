package com.eddndev.purpura.data.remote.interceptor

import com.eddndev.purpura.data.session.TokenStore
import com.eddndev.purpura.domain.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject

// Adjunta `Authorization: Bearer <token>` a toda peticion protegida (contrato §1.3/§3).
// Excluye solo las rutas PUBLICAS (register/login/google y /health), que no requieren token. OJO: no
// se puede excluir por "contiene /auth/" porque /auth/me y /auth/verify-email/request SI requieren
// Bearer; por eso la exclusion es un allowlist explicito, no un prefijo. Ademas, si un endpoint
// PROTEGIDO responde 401 (token expirado/invalido) habiendo token, INVALIDA la sesion: asi el gate de
// MainActivity (que observa la sesion) lleva a Auth en vez de dejar al usuario atascado. El 401 de las
// rutas publicas (credenciales invalidas en login) NO entra aqui porque esos paths retornan antes.
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionRepository: SessionRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (isPublicApiPath(request.url.encodedPath)) {
            return chain.proceed(request)
        }
        val token = tokenStore.peekToken()
        val authorized = if (token.isNullOrEmpty()) {
            request
        } else {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        }
        val response = chain.proceed(authorized)
        // Solo si enviamos un token y el servidor lo rechaza: la sesion ya no sirve -> invalidar.
        if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED && !token.isNullOrEmpty()) {
            sessionRepository.invalidate()
        }
        return response
    }

}

// Rutas sin Bearer: las credenciales viajan en el cuerpo (register/login/google) o no hace falta
// sesion (/health). Todo lo demas bajo /auth/* (me, verify-email/request) y /account lleva token.
// Top-level e internal a proposito: deja fijar en pruebas la invariante de seguridad (que /auth/me y
// /auth/verify-email/request SI lleven token, y que las publicas NO) sin instanciar el interceptor
// (que depende de TokenStore con Context cifrado).
internal fun isPublicApiPath(path: String): Boolean =
    path.endsWith("/health") || PUBLIC_AUTH_PATHS.any { path.endsWith(it) }

private val PUBLIC_AUTH_PATHS = listOf(
    "/auth/register",
    "/auth/login",
    "/auth/google",
)
