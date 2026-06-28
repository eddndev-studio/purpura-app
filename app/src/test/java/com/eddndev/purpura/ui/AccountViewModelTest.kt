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
import com.eddndev.purpura.domain.usecase.auth.LinkGoogleUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.domain.usecase.auth.UnlinkGoogleUseCase
import com.eddndev.purpura.ui.account.AccountViewModel
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
        linkGoogle = LinkGoogleUseCase(authRepository, sessionRepository),
        unlinkGoogle = UnlinkGoogleUseCase(authRepository, sessionRepository),
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
    fun `un 404 del backend se trata como exito y limpia la sesion`() = runTest(dispatcher) {
        // Borrado idempotente: si la cuenta ya no existe (404 user_not_found), igual se limpia la
        // sesion local en vez de dejar al usuario atrapado con un aviso.
        sessionRepository.sessionFlow.value = sampleSession()
        authRepository.deleteError = DomainError.UserNotFound
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.deleteAccount()

        assertTrue(sessionRepository.cleared)
        assertNull(sessionRepository.sessionFlow.value)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `toques repetidos durante el borrado se ignoran`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        authRepository.deleteGate = gate
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.deleteAccount() // queda en vuelo (suspendido en el gate)
        assertTrue(viewModel.uiState.value.isDeletingAccount)
        viewModel.deleteAccount() // el guard lo ignora mientras hay uno en curso

        assertEquals(1, authRepository.deleteCalls)

        gate.complete(Unit) // deja terminar el primero
        assertTrue(sessionRepository.cleared)
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

    @Test
    fun `vincular Google exitoso refresca la sesion y apaga el progreso`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession(googleLinked = false)
        authRepository.linkResult = linkedUser(googleLinked = true)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.linkGoogle("google-id-token")

        assertEquals(listOf("google-id-token"), authRepository.linkCalls)
        // La sesion se refresca CONSERVANDO el token: la fila pasa a "vinculado" sin re-login.
        assertTrue(sessionRepository.sessionFlow.value?.user?.googleLinked == true)
        assertEquals("tok", sessionRepository.sessionFlow.value?.token)
        val state = viewModel.uiState.value
        assertFalse(state.isUpdatingGoogleLink)
        assertNull(state.errorRes)
    }

    @Test
    fun `vincular Google en conflicto conserva la sesion y muestra el aviso`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession(googleLinked = false)
        authRepository.linkError = DomainError.GoogleLinkConflict
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.linkGoogle("google-id-token")

        // El backend rechazo: la sesion no cambia y se muestra el aviso especifico.
        assertFalse(sessionRepository.sessionFlow.value?.user?.googleLinked ?: true)
        assertTrue(sessionRepository.updatedUsers.isEmpty())
        val state = viewModel.uiState.value
        assertFalse(state.isUpdatingGoogleLink)
        assertEquals(R.string.error_google_link_conflict, state.errorRes)
    }

    @Test
    fun `desvincular Google exitoso refresca la sesion`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession(googleLinked = true)
        authRepository.unlinkResult = linkedUser(googleLinked = false)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.unlinkGoogle()

        assertEquals(1, authRepository.unlinkCalls)
        assertFalse(sessionRepository.sessionFlow.value?.user?.googleLinked ?: true)
        assertFalse(viewModel.uiState.value.isUpdatingGoogleLink)
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `desvincular sin contrasena muestra el aviso especifico`() = runTest(dispatcher) {
        sessionRepository.sessionFlow.value = sampleSession(googleLinked = true)
        authRepository.unlinkError = DomainError.CannotUnlinkGoogle
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.session.collect {} }

        viewModel.unlinkGoogle()

        assertEquals(R.string.error_cannot_unlink_google, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isUpdatingGoogleLink)
    }

    @Test
    fun `un fallo del selector de Google muestra aviso sin tocar el backend`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.googleSignInFailed()

        assertEquals(R.string.account_link_google_failed, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isUpdatingGoogleLink)
        assertTrue(authRepository.linkCalls.isEmpty())
    }

    @Test
    fun `no se puede borrar la cuenta mientras se vincula Google`() = runTest(dispatcher) {
        // Guard cruzado isBusy: una vinculacion en vuelo bloquea el borrado (y viceversa).
        sessionRepository.sessionFlow.value = sampleSession(googleLinked = false)
        val gate = CompletableDeferred<Unit>()
        authRepository.linkGate = gate
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.linkGoogle("tok") // queda en vuelo (suspendido en el gate)
        assertTrue(viewModel.uiState.value.isUpdatingGoogleLink)
        viewModel.deleteAccount() // el guard lo ignora mientras hay una vinculacion en curso

        assertEquals(0, authRepository.deleteCalls)

        gate.complete(Unit) // deja terminar la vinculacion
        assertTrue(sessionRepository.sessionFlow.value?.user?.googleLinked == true)
    }

    private fun sampleSession(googleLinked: Boolean = false) = Session(
        token = "tok",
        user = User(
            id = "u1",
            email = "ana@example.com",
            nombre = "Ana",
            authProvider = AuthProvider.password,
            googleLinked = googleLinked,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        ),
    )
}

// Fake de AuthRepository: deleteAccount y link/unlink Google son los relevantes (cuentan llamadas y
// pueden fallar); el resto de metodos no se ejercita en estas pruebas.
private class FakeAuthRepository : AuthRepository {
    var deleteCalls = 0
    var deleteError: Throwable? = null

    // Si se setea, deleteAccount() se suspende en este gate tras contar la llamada: permite
    // probar el guard de re-entrada con un borrado "en vuelo".
    var deleteGate: CompletableDeferred<Unit>? = null

    val linkCalls = mutableListOf<String>()
    var linkError: Throwable? = null
    var linkResult: User = linkedUser(googleLinked = true)
    var linkGate: CompletableDeferred<Unit>? = null
    var unlinkCalls = 0
    var unlinkError: Throwable? = null
    var unlinkResult: User = linkedUser(googleLinked = false)

    override suspend fun register(email: String, nombre: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun login(email: String, password: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun loginWithGoogle(idToken: String): AuthResult =
        error("no usado en estas pruebas")

    override suspend fun deleteAccount() {
        deleteCalls++
        deleteGate?.await()
        deleteError?.let { throw it }
    }

    override suspend fun linkGoogle(idToken: String): User {
        linkCalls += idToken
        linkGate?.await()
        linkError?.let { throw it }
        return linkResult
    }

    override suspend fun unlinkGoogle(): User {
        unlinkCalls++
        unlinkError?.let { throw it }
        return unlinkResult
    }
}

private fun linkedUser(googleLinked: Boolean) = User(
    id = "u1",
    email = "ana@example.com",
    nombre = "Ana",
    authProvider = AuthProvider.password,
    googleLinked = googleLinked,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
)

// Fake de SessionRepository: expone la sesion como flujo y registra si se limpio (clear() emite null,
// lo que en produccion dispara la navegacion a Auth) o si se refresco el usuario (updateUser, que
// conserva el token).
private class FakeSessionRepository : SessionRepository {
    val sessionFlow = MutableStateFlow<Session?>(null)
    var cleared = false
    val updatedUsers = mutableListOf<User>()

    override fun observeSession(): Flow<Session?> = sessionFlow

    override suspend fun persist(result: AuthResult) = error("no usado en estas pruebas")

    override suspend fun updateUser(user: User) {
        updatedUsers += user
        sessionFlow.value?.let { sessionFlow.value = it.copy(user = user) }
    }

    override suspend fun currentToken(): String? = sessionFlow.value?.token

    override suspend fun clear() {
        cleared = true
        sessionFlow.value = null
    }

    override fun invalidate() {
        sessionFlow.value = null
    }
}
