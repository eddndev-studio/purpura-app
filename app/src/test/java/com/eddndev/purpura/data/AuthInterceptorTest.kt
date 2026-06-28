package com.eddndev.purpura.data

import com.eddndev.purpura.data.remote.interceptor.isPublicApiPath
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Fija la invariante de seguridad del AuthInterceptor: las rutas autenticadas bajo /auth/* (me y
// verify-email/request) DEBEN llevar Bearer (no son publicas), mientras que register/login/google y
// /health no. Un fallo aqui = o se filtra el token a una ruta publica, o /auth/me viaja sin token y
// el backend devuelve 401 (y el interceptor invalidaria la sesion de mas).
class AuthInterceptorTest {

    private val base = "/api/v1"

    @Test
    fun `me y verify-email request NO son publicas (llevan Bearer)`() {
        assertFalse(isPublicApiPath("$base/auth/me"))
        assertFalse(isPublicApiPath("$base/auth/verify-email/request"))
    }

    @Test
    fun `register login google y health son publicas (sin Bearer)`() {
        assertTrue(isPublicApiPath("$base/auth/register"))
        assertTrue(isPublicApiPath("$base/auth/login"))
        assertTrue(isPublicApiPath("$base/auth/google"))
        assertTrue(isPublicApiPath("/health"))
    }

    @Test
    fun `cuenta y eventos no son publicas`() {
        assertFalse(isPublicApiPath("$base/account/link-google"))
        assertFalse(isPublicApiPath("$base/events"))
    }
}
