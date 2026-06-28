package com.eddndev.purpura.ui

import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.domain.usecase.auth.RefreshCurrentUserUseCase
import com.eddndev.purpura.domain.usecase.auth.RequestEmailVerificationUseCase
import com.eddndev.purpura.ui.verification.EmailVerificationViewModel
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

// Prueba el aviso "verifica tu correo": que se muestre solo si el usuario esta sin verificar, que el
// envio marque "enviado" (con guard de re-entrada) o muestre el error inline, y que el refresh
// (GET /auth/me) lo oculte cuando el correo ya se confirmo. Mismo andamiaje que el resto de VMs.
@OptIn(ExperimentalCoroutinesApi::class)
class EmailVerificationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val authRepository = FakeVerifAuthRepository()
    private val sessionRepository = FakeVerifSessionRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = EmailVerificationViewModel(
        observeSession = ObserveSessionUseCase(sessionRepository),
        requestEmailVerification = RequestEmailVerificationUseCase(authRepository),
        refreshCurrentUser = RefreshCurrentUserUseCase(authRepository, sessionRepository),
    )

    @Test
    fun `usuario sin verificar muestra el banner con su correo`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state.visible)
        assertEquals("ana@example.com", state.email)
    }

    @Test
    fun `usuario verificado no muestra el banner`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = true))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.visible)
    }

    @Test
    fun `sin sesion no muestra el banner`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = null
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertFalse(viewModel.uiState.value.visible)
    }

    @Test
    fun `enviar pide el correo y marca enviado`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.requestVerification()

        assertEquals(1, authRepository.requestVerificationCalls)
        val state = viewModel.uiState.value
        assertTrue(state.sent)
        assertFalse(state.isSending)
        assertNull(state.errorRes)
    }

    @Test
    fun `enviar con fallo de red muestra el error inline y no marca enviado`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.requestVerificationError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.requestVerification()

        val state = viewModel.uiState.value
        assertFalse(state.sent)
        assertFalse(state.isSending)
        assertEquals(R.string.error_network, state.errorRes)
    }

    @Test
    fun `toques repetidos durante el envio se ignoran`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        val gate = CompletableDeferred<Unit>()
        authRepository.requestGate = gate
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.requestVerification() // queda en vuelo (suspendido en el gate)
        assertTrue(viewModel.uiState.value.isSending)
        viewModel.requestVerification() // el guard lo ignora mientras hay uno en curso

        assertEquals(1, authRepository.requestVerificationCalls)

        gate.complete(Unit)
        assertTrue(viewModel.uiState.value.sent)
    }

    @Test
    fun `refresh detecta el correo confirmado y oculta el banner`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.meResult = user(emailVerified = true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.visible)

        viewModel.refresh()

        assertEquals(1, authRepository.meCalls)
        assertFalse(viewModel.uiState.value.visible)
    }

    @Test
    fun `refresh limpia el estado enviado al reanudar`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.meResult = user(emailVerified = false)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.requestVerification()
        assertTrue(viewModel.uiState.value.sent)

        viewModel.refresh()

        // Un "enviado" no debe sobrevivir a la navegacion: al volver a primer plano vuelve al base.
        assertFalse(viewModel.uiState.value.sent)
    }

    @Test
    fun `refresh limpia un error transitorio al reanudar`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.meResult = user(emailVerified = false)
        authRepository.requestVerificationError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.requestVerification()
        assertEquals(R.string.error_network, viewModel.uiState.value.errorRes)

        viewModel.refresh()

        // Un error de hace rato no debe reaparecer tras navegar y volver.
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `refresh con error es silencioso y conserva la sesion`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.meError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.refresh()

        // Un /auth/me fallido no es accion del usuario: no se muestra error y el aviso sigue igual.
        val state = viewModel.uiState.value
        assertNull(state.errorRes)
        assertTrue(state.visible)
        assertTrue(sessionRepository.updatedUsers.isEmpty())
    }

    @Test
    fun `errorShown limpia el aviso inline`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = session(user(emailVerified = false))
        authRepository.requestVerificationError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.requestVerification()
        assertEquals(R.string.error_network, viewModel.uiState.value.errorRes)

        viewModel.errorShown()

        assertNull(viewModel.uiState.value.errorRes)
    }

    private fun user(emailVerified: Boolean) = User(
        id = "u1",
        email = "ana@example.com",
        nombre = "Ana",
        authProvider = AuthProvider.password,
        googleLinked = false,
        emailVerified = emailVerified,
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    private fun session(user: User) = Session(token = "tok", user = user)
}

// Fake de AuthRepository: me y requestEmailVerification son los relevantes (cuentan llamadas y pueden
// fallar/suspenderse); el resto no se ejercita.
private class FakeVerifAuthRepository : AuthRepository {
    var meResult: User? = null
    var meError: Throwable? = null
    var meCalls = 0
    var requestVerificationError: Throwable? = null
    var requestVerificationCalls = 0

    // Si se setea, requestEmailVerification() se suspende aqui tras contar la llamada: permite probar
    // el guard de re-entrada con un envio "en vuelo".
    var requestGate: CompletableDeferred<Unit>? = null

    override suspend fun register(email: String, nombre: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun login(email: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun loginWithGoogle(idToken: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun deleteAccount() = error("no usado en estas pruebas")

    override suspend fun linkGoogle(idToken: String): User = error("no usado en estas pruebas")

    override suspend fun unlinkGoogle(): User = error("no usado en estas pruebas")

    override suspend fun me(): User {
        meCalls++
        meError?.let { throw it }
        return requireNotNull(meResult) { "meResult no configurado" }
    }

    override suspend fun requestEmailVerification() {
        requestVerificationCalls++
        requestGate?.await()
        requestVerificationError?.let { throw it }
    }
}

// Fake de SessionRepository: expone la sesion como flujo y registra el refresco de usuario.
private class FakeVerifSessionRepository : SessionRepository {
    val sessionFlow = MutableStateFlow<Session?>(null)
    val updatedUsers = mutableListOf<User>()

    override fun observeSession(): Flow<Session?> = sessionFlow

    override suspend fun persist(result: AuthResult) = error("no usado en estas pruebas")

    override suspend fun updateUser(user: User) {
        updatedUsers += user
        sessionFlow.value?.let { sessionFlow.value = it.copy(user = user) }
    }

    override suspend fun currentToken(): String? = sessionFlow.value?.token

    override suspend fun clear() = error("no usado en estas pruebas")

    override fun invalidate() {
        sessionFlow.value = null
    }
}
