package com.eddndev.purpura.domain.support

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import com.eddndev.purpura.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.LocalDate

// Datos de prueba reutilizables.
object TestData {
    val user = User(
        id = "u1",
        email = "ana@example.com",
        nombre = "Ana",
        authProvider = AuthProvider.password,
        googleLinked = false,
        createdAt = Instant.EPOCH,
    )

    fun event(
        id: String = "e1",
        status: EventStatus = EventStatus.pendiente,
        reminder: Reminder = Reminder.ten_minutes_before,
        startsAt: Instant = Instant.parse("2026-06-10T15:30:00Z"),
    ): Event = Event(
        id = id,
        userId = "u1",
        type = EventType.junta,
        contact = Contact("Maria", null),
        location = Location(19.4, -99.1, null),
        description = "Revision",
        startsAt = startsAt,
        status = status,
        reminder = reminder,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    val draft = NewEventDraft(
        type = EventType.junta,
        contact = Contact("Maria", null),
        location = Location(19.4, -99.1, null),
        description = "Revision",
        startsAt = Instant.parse("2026-06-10T15:30:00Z"),
        reminder = Reminder.at_time,
    )

    fun authResult(token: String = "jwt-token") =
        AuthResult(accessToken = token, expiresInSeconds = 86_400, user = user)
}

// Fake del puerto de eventos: registra llamadas y devuelve valores configurados.
class FakeEventRepository : EventRepository {
    var createResult: Event? = null
    var changeStatusResult: Event? = null
    var updateResult: Event? = null
    var getByIdResult: Event? = null
    var queryResult: Page<Event>? = null

    val upcomingFlow = MutableStateFlow<List<Event>>(emptyList())
    val monthFlow = MutableStateFlow<List<Event>>(emptyList())

    val createdDrafts = mutableListOf<NewEventDraft>()
    val statusChanges = mutableListOf<Pair<String, EventStatus>>()
    val patches = mutableListOf<Pair<String, EventPatch>>()
    val deletedIds = mutableListOf<String>()
    var lastUpcomingArgs: Pair<LocalDate, Int>? = null
    var lastRefreshRange: Pair<LocalDate, LocalDate>? = null
    var refreshRangeCount = 0
    var lastQuery: EventQuery? = null
    var queryCallCount = 0

    override fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>> {
        lastUpcomingArgs = today to daysAhead
        return upcomingFlow
    }

    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> = monthFlow

    override suspend fun refreshRange(from: LocalDate, to: LocalDate) {
        refreshRangeCount++
        lastRefreshRange = from to to
    }

    override suspend fun query(query: EventQuery): Page<Event> {
        queryCallCount++
        lastQuery = query
        return requireNotNull(queryResult) { "queryResult no configurado" }
    }

    override suspend fun getById(id: String): Event =
        requireNotNull(getByIdResult) { "getByIdResult no configurado" }

    override suspend fun create(draft: NewEventDraft): Event {
        createdDrafts += draft
        return requireNotNull(createResult) { "createResult no configurado" }
    }

    override suspend fun update(id: String, patch: EventPatch): Event {
        patches += id to patch
        return requireNotNull(updateResult) { "updateResult no configurado" }
    }

    override suspend fun changeStatus(id: String, status: EventStatus): Event {
        statusChanges += id to status
        return requireNotNull(changeStatusResult) { "changeStatusResult no configurado" }
    }

    override suspend fun delete(id: String) {
        deletedIds += id
    }

    override suspend fun export(query: EventQuery?): ExportDocument =
        error("export no usado en estas pruebas")

    override suspend fun import(request: ImportRequest): ImportResult =
        error("import no usado en estas pruebas")
}

// Fake del programador de recordatorios: registra schedule/cancel.
class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Event>()
    val cancelled = mutableListOf<String>()

    override fun schedule(event: Event) {
        scheduled += event
    }

    override fun cancel(eventId: String) {
        cancelled += eventId
    }
}

// Fake del puerto de autenticacion.
class FakeAuthRepository : AuthRepository {
    var loginResult: AuthResult? = null
    var loginError: Throwable? = null
    val loginCalls = mutableListOf<Pair<String, String>>()
    var deleteAccountError: Throwable? = null
    var deleteAccountCalls = 0

    // Vincular/Desvincular Google: el resultado por defecto es TestData.user; las pruebas pueden
    // sustituirlo o forzar un error, y se registran las llamadas (idTokens / conteo).
    var linkGoogleResult: User = TestData.user
    var linkGoogleError: Throwable? = null
    val linkGoogleCalls = mutableListOf<String>()
    var unlinkGoogleResult: User = TestData.user
    var unlinkGoogleError: Throwable? = null
    var unlinkGoogleCalls = 0

    override suspend fun register(email: String, nombre: String, password: String): AuthResult =
        requireNotNull(loginResult) { "loginResult no configurado" }

    override suspend fun login(email: String, password: String): AuthResult {
        loginCalls += email to password
        loginError?.let { throw it }
        return requireNotNull(loginResult) { "loginResult no configurado" }
    }

    override suspend fun loginWithGoogle(idToken: String): AuthResult =
        requireNotNull(loginResult) { "loginResult no configurado" }

    override suspend fun deleteAccount() {
        deleteAccountCalls++
        deleteAccountError?.let { throw it }
    }

    override suspend fun linkGoogle(idToken: String): User {
        linkGoogleCalls += idToken
        linkGoogleError?.let { throw it }
        return linkGoogleResult
    }

    override suspend fun unlinkGoogle(): User {
        unlinkGoogleCalls++
        unlinkGoogleError?.let { throw it }
        return unlinkGoogleResult
    }
}

// Fake del puerto de sesion.
class FakeSessionRepository : SessionRepository {
    val persisted = mutableListOf<AuthResult>()
    val updatedUsers = mutableListOf<User>()
    var clearCount = 0
    val sessionFlow = MutableStateFlow<Session?>(null)

    override fun observeSession(): Flow<Session?> = sessionFlow

    override suspend fun persist(result: AuthResult) {
        persisted += result
        sessionFlow.value = Session(result.accessToken, result.user)
    }

    override suspend fun updateUser(user: User) {
        updatedUsers += user
        sessionFlow.value?.let { sessionFlow.value = it.copy(user = user) }
    }

    override suspend fun currentToken(): String? = sessionFlow.value?.token

    override suspend fun clear() {
        clearCount++
        sessionFlow.value = null
    }

    override fun invalidate() {
        sessionFlow.value = null
    }
}
