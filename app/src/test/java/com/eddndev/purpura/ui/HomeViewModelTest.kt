package com.eddndev.purpura.ui

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.usecase.home.GetUpcomingEventsUseCase
import com.eddndev.purpura.domain.usecase.home.RefreshUpcomingEventsUseCase
import com.eddndev.purpura.ui.home.HomeViewModel
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
import java.time.LocalDate

// Prueba la maquina de estados de HomeViewModel: combine(cache, refreshing, error) + el refresh
// imperativo. Usa UnconfinedTestDispatcher para que el launch del init corra de inmediato.
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = HomeViewModel(
        getUpcomingEvents = GetUpcomingEventsUseCase(repository),
        refreshUpcomingEvents = RefreshUpcomingEventsUseCase(repository),
    )

    @Test
    fun `arranque en frio muestra el contenido del cache sin error`() = runTest(dispatcher) {
        repository.upcomingFlow.value = listOf(sampleEvent)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(listOf(sampleEvent), state.events)
        assertFalse(state.isLoading)
        assertNull(state.errorRes)
        assertFalse(state.isEmpty)
    }

    @Test
    fun `un refresh fallido conserva el cache y emite un aviso`() = runTest(dispatcher) {
        repository.upcomingFlow.value = listOf(sampleEvent)
        repository.refreshError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(listOf(sampleEvent), state.events)
        assertNotNull(state.errorRes)
        assertFalse(state.isLoading)
    }

    @Test
    fun `cache vacio tras cargar muestra el estado vacio`() = runTest(dispatcher) {
        repository.upcomingFlow.value = emptyList()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state.events.isEmpty())
        assertFalse(state.isLoading)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `observa y sincroniza exactamente la misma ventana`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val observed = repository.lastUpcomingArgs
        val refreshed = repository.lastRefreshRange
        assertNotNull(observed)
        assertNotNull(refreshed)
        assertEquals(observed!!.first, refreshed!!.first)
        assertEquals(refreshed.first.plusDays(4), refreshed.second)
    }

    private val sampleEvent = Event(
        id = "e1",
        userId = "u1",
        type = EventType.junta,
        contact = Contact("Maria", null),
        location = Location(19.4, -99.1, null),
        description = "Revision",
        startsAt = Instant.parse("2026-06-10T15:30:00Z"),
        status = EventStatus.pendiente,
        reminder = Reminder.ten_minutes_before,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )
}

// Fake minimo del puerto de eventos para probar HomeViewModel (observe + refresh).
private class FakeEventRepository : EventRepository {
    val upcomingFlow = MutableStateFlow<List<Event>>(emptyList())
    var refreshError: Throwable? = null
    var lastUpcomingArgs: Pair<LocalDate, Int>? = null
    var lastRefreshRange: Pair<LocalDate, LocalDate>? = null

    override fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>> {
        lastUpcomingArgs = today to daysAhead
        return upcomingFlow
    }

    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> = upcomingFlow

    override suspend fun refreshRange(from: LocalDate, to: LocalDate) {
        lastRefreshRange = from to to
        refreshError?.let { throw it }
    }

    override suspend fun query(query: EventQuery): Page<Event> = notUsed()
    override suspend fun getById(id: String): Event = notUsed()
    override suspend fun create(draft: NewEventDraft): Event = notUsed()
    override suspend fun update(id: String, patch: EventPatch): Event = notUsed()
    override suspend fun changeStatus(id: String, status: EventStatus): Event = notUsed()
    override suspend fun delete(id: String) = notUsed()
    override suspend fun export(query: EventQuery?): ExportDocument = notUsed()
    override suspend fun import(request: ImportRequest): ImportResult = notUsed()

    private fun notUsed(): Nothing = error("no usado en HomeViewModelTest")
}
