package com.eddndev.purpura.data

import com.eddndev.purpura.data.remote.dto.UserDto
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.domain.model.AuthProvider
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas del mapper de autenticacion. Invariantes que fija: (1) googleLinked: una cuenta de ORIGEN
// Google siempre esta vinculada (el backend garantiza google_sub != null), asi que el mapper lo
// reconstruye aunque el DTO diga lo contrario; para password se respeta el DTO. (2) emailVerified:
// se mapea tal cual del backend, y un cache viejo (JSON sin la clave) se asume verificado (default
// true) para no mostrar el aviso de verificacion de mas.
class AuthRemoteMapperTest {

    private val mapper = AuthRemoteMapper()

    private fun userDto(
        authProvider: String,
        googleLinked: Boolean,
        emailVerified: Boolean = true,
    ) = UserDto(
        id = "u1",
        email = "ana@example.com",
        nombre = "Ana",
        authProvider = authProvider,
        googleLinked = googleLinked,
        emailVerified = emailVerified,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `password sin vincular mapea a googleLinked false`() {
        val user = mapper.toUser(userDto("password", googleLinked = false))
        assertEquals(AuthProvider.password, user.authProvider)
        assertFalse(user.googleLinked)
    }

    @Test
    fun `password vinculado respeta el DTO`() {
        val user = mapper.toUser(userDto("password", googleLinked = true))
        assertTrue(user.googleLinked)
    }

    @Test
    fun `origen Google se reconstruye como vinculado aunque el DTO diga false`() {
        // Un cache de una version anterior deserializa googleLinked=false (clave ausente -> default).
        // Una cuenta de origen Google DEBE verse vinculada igualmente, para no ofrecerle "Vincular".
        val user = mapper.toUser(userDto("google", googleLinked = false))
        assertEquals(AuthProvider.google, user.authProvider)
        assertTrue(user.googleLinked)
    }

    @Test
    fun `toUserDto conserva googleLinked para el cache`() {
        val user = mapper.toUser(userDto("password", googleLinked = true))
        assertTrue(mapper.toUserDto(user).googleLinked)
    }

    @Test
    fun `emailVerified se mapea tal cual desde el DTO`() {
        assertFalse(mapper.toUser(userDto("password", googleLinked = false, emailVerified = false)).emailVerified)
        assertTrue(mapper.toUser(userDto("password", googleLinked = false, emailVerified = true)).emailVerified)
    }

    @Test
    fun `toUserDto conserva emailVerified para el cache`() {
        val user = mapper.toUser(userDto("password", googleLinked = false, emailVerified = false))
        assertFalse(mapper.toUserDto(user).emailVerified)
    }

    @Test
    fun `cache viejo sin la clave emailVerified se asume verificado`() {
        // JSON de una version anterior (sin emailVerified): Moshi aplica el default del data class.
        // Debe quedar true para NO mostrar el aviso "verifica tu correo" a quien quiza ya esta
        // verificado (o es de origen Google); el valor real llega luego por login/me.
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val json = """{"id":"u1","email":"ana@example.com","nombre":"Ana",""" +
            """"authProvider":"password","googleLinked":false,"createdAt":"2026-01-01T00:00:00Z"}"""
        val dto = moshi.adapter(UserDto::class.java).fromJson(json)!!

        assertTrue(dto.emailVerified)
        assertTrue(mapper.toUser(dto).emailVerified)
    }
}
