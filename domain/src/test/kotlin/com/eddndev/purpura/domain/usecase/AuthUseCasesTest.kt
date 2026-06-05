package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.support.FakeAuthRepository
import com.eddndev.purpura.domain.support.FakeSessionRepository
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.auth.LoginUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
