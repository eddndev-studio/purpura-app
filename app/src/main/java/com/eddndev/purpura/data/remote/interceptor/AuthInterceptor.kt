package com.eddndev.purpura.data.remote.interceptor

import com.eddndev.purpura.data.session.TokenStore
import com.eddndev.purpura.domain.repository.SessionRepository
import okhttp3.Interceptor
import okhttp3.Response
import java.net.HttpURLConnection
import javax.inject.Inject

// Adjunta `Authorization: Bearer <token>` a toda peticion protegida (contrato §1.3/§3).
// Excluye /auth/* y /health, que no requieren token. Ademas, si un endpoint PROTEGIDO responde 401
// (token expirado/invalido) habiendo token, INVALIDA la sesion: asi el gate de MainActivity (que
// observa la sesion) lleva a Auth en vez de dejar al usuario atascado con llamadas fallando. El 401
// de /auth/* (credenciales invalidas en login) NO entra aqui porque esos paths retornan antes.
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    private val sessionRepository: SessionRepository,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        if (path.contains("/auth/") || path.endsWith("/health")) {
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
