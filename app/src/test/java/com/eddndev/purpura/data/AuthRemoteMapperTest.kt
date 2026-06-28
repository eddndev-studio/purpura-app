package com.eddndev.purpura.data

import com.eddndev.purpura.data.remote.dto.UserDto
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.domain.model.AuthProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// Pruebas del mapper de autenticacion, centradas en la invariante de googleLinked: una cuenta de
// ORIGEN Google siempre esta vinculada (el backend garantiza google_sub != null), asi que el mapper
// lo reconstruye aunque el DTO diga lo contrario; para una cuenta password se respeta el DTO.
class AuthRemoteMapperTest {

    private val mapper = AuthRemoteMapper()

    private fun userDto(authProvider: String, googleLinked: Boolean) = UserDto(
        id = "u1",
        email = "ana@example.com",
        nombre = "Ana",
        authProvider = authProvider,
        googleLinked = googleLinked,
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
}
