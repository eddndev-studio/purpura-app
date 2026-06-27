package com.eddndev.purpura.ui

import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import com.eddndev.purpura.domain.usecase.auth.DeleteAccountUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.ui.account.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

// Prueba el borrado de cuenta de AccountViewModel: el camino feliz limpia la sesion local (que es
// lo que dispara la navegacion a Auth) y el fallo de red la CONSERVA y muestra el aviso. Mismo
// andamiaje que el resto (UnconfinedTestDispatcher + setMain).
@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val authRepository = FakeAuthRepository()
    private val sessionRepository = FakeSessionRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = AccountViewModel(
        observeSession = ObserveSessionUseCase(sessionRepository),
        logout = LogoutUseCase(sessionRepository),
        deleteAccount = DeleteAccountUseCase(authRepository, sessionRepository),
    )

    @Test
    fun `borrar cuenta exitoso borra remoto y limpia la sesion`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.deleteAccount()

        assertEquals(1, authRepository.deleteCalls)
        assertTrue(sessionRepository.cleared)
        assertNull(sessionRepository.sessionFlow.value)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `fallo de red conserva la sesion y muestra el aviso`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession()
        authRepository.deleteError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.deleteAccount()

        // El borrado remoto fallo: NO se limpia la sesion (el usuario sigue dentro).
        assertFalse(sessionRepository.cleared)
        assertNotNull(sessionRepository.sessionFlow.value)
        val state = viewModel.uiState.value
        assertFalse(state.isDeletingAccount)
        assertEquals(R.string.error_network, state.errorRes)
    }

    @Test
    fun `errorShown limpia el aviso`() = runTest(dispatcher) {
        authRepository.deleteError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.deleteAccount()
        assertNotNull(viewModel.uiState.value.errorRes)

        viewModel.errorShown()

        assertNull(viewModel.uiState.value.errorRes)
    }

    private fun sampleSession() = Session(
        token = "tok",
        user = User(
            id = "u1",
            email = "ana@example.com",
            nombre = "Ana",
            authProvider = AuthProvider.password,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
    )
}

// Fake de AuthRepository: solo deleteAccount es relevante (cuenta llamadas y puede fallar); el
// resto de metodos no se ejercita en estas pruebas.
private class FakeAuthRepository : AuthRepository {
    var deleteCalls = 0
    var deleteError: Throwable? = null

    override suspend fun register(email: String, nombre: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun login(email: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun loginWithGoogle(idToken: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun deleteAccount() {
        deleteCalls++
        deleteError?.let { throw it }
    }
}

// Fake de SessionRepository: expone la sesion como flujo y registra si se limpio. clear() emite
// null (lo que en produccion dispara la navegacion a Auth).
private class FakeSessionRepository : SessionRepository {
    val sessionFlow = MutableStateFlow<Session?>(null)
    var cleared = false

    override fun observeSession(): Flow<Session?> = sessionFlow

    override suspend fun persist(result: AuthResult) = error("no usado en estas pruebas")

    override suspend fun currentToken(): String? = sessionFlow.value?.token

    override suspend fun clear() {
        cleared = true
        sessionFlow.value = null
    }

    override fun invalidate() {
        sessionFlow.value = null
    }
}
