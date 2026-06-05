package com.eddndev.purpura.data.remote.interceptor

import com.eddndev.purpura.data.session.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

// Adjunta `Authorization: Bearer <token>` a toda peticion protegida (contrato §1.3/§3).
// Excluye /auth/* y /health, que no requieren token.
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
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
        return chain.proceed(authorized)
    }
}
