package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.support.FakeAuthRepository
import com.eddndev.purpura.domain.support.FakeSessionRepository
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.auth.LinkGoogleUseCase
import com.eddndev.purpura.domain.usecase.auth.LoginUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.UnlinkGoogleUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {

    private val authRepository = FakeAuthRepository()
    private val sessionRepository = FakeSessionRepository()

    @Test
    fun `login exitoso persiste la sesion y normaliza el correo`() = runTest {
        val result = TestData.authResult()
        authRepository.loginResult = result
        val useCase = LoginUseCase(authRepository, sessionRepository)

        val returned = useCase("  ana@example.com  ", "secret123")

        assertEquals(result, returned)
        assertEquals(listOf("ana@example.com" to "secret123"), authRepository.loginCalls)
        assertEquals(listOf(result), sessionRepository.persisted)
    }

    @Test
    fun `login con credenciales invalidas propaga el error sin persistir`() = runTest {
        authRepository.loginError = DomainError.InvalidCredential
        val useCase = LoginUseCase(authRepository, sessionRepository)

        val error = runCatching { useCase("ana@example.com", "wrong") }.exceptionOrNull()

        assertTrue(error is DomainError.InvalidCredential)
        assertTrue(sessionRepository.persisted.isEmpty())
    }

    @Test
    fun `logout limpia la sesion`() = runTest {
        sessionRepository.persist(TestData.authResult())
        val useCase = LogoutUseCase(sessionRepository)

        useCase()

        assertEquals(1, sessionRepository.clearCount)
        assertNull(sessionRepository.sessionFlow.value)
    }

    @Test
    fun `vincular Google adjunta el idToken y refresca el usuario cacheado`() = runTest {
        sessionRepository.persist(TestData.authResult())
        val linked = TestData.user.copy(googleLinked = true)
        authRepository.linkGoogleResult = linked
        val useCase = LinkGoogleUseCase(authRepository, sessionRepository)

        val returned = useCase("google-id-token")

        assertEquals(linked, returned)
        assertEquals(listOf("google-id-token"), authRepository.linkGoogleCalls)
        // El usuario cacheado se actualiza CONSERVANDO el token (no se vuelve a iniciar sesion).
        assertEquals(listOf(linked), sessionRepository.updatedUsers)
        assertTrue(sessionRepository.sessionFlow.value?.user?.googleLinked == true)
    }

    @Test
    fun `vincular Google en conflicto propaga el error sin tocar la sesion`() = runTest {
        sessionRepository.persist(TestData.authResult())
        authRepository.linkGoogleError = DomainError.GoogleLinkConflict
        val useCase = LinkGoogleUseCase(authRepository, sessionRepository)

        val error = runCatching { useCase("google-id-token") }.exceptionOrNull()

        assertTrue(error is DomainError.GoogleLinkConflict)
        assertTrue(sessionRepository.updatedUsers.isEmpty())
    }

    @Test
    fun `desvincular Google refresca el usuario cacheado`() = runTest {
        sessionRepository.persist(TestData.authResult().copy(user = TestData.user.copy(googleLinked = true)))
        val unlinked = TestData.user.copy(googleLinked = false)
        authRepository.unlinkGoogleResult = unlinked
        val useCase = UnlinkGoogleUseCase(authRepository, sessionRepository)

        val returned = useCase()

        assertEquals(unlinked, returned)
        assertEquals(1, authRepository.unlinkGoogleCalls)
        assertEquals(listOf(unlinked), sessionRepository.updatedUsers)
        assertFalse(sessionRepository.sessionFlow.value?.user?.googleLinked ?: true)
    }

    @Test
    fun `desvincular Google sin contrasena propaga el error sin tocar la sesion`() = runTest {
        sessionRepository.persist(TestData.authResult())
        authRepository.unlinkGoogleError = DomainError.CannotUnlinkGoogle
        val useCase = UnlinkGoogleUseCase(authRepository, sessionRepository)

        val error = runCatching { useCase() }.exceptionOrNull()

        assertTrue(error is DomainError.CannotUnlinkGoogle)
        assertTrue(sessionRepository.updatedUsers.isEmpty())
    }
}
