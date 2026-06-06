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
import com.eddndev.purpura.domain.usecase.calendar.GetMonthEventsUseCase
import com.eddndev.purpura.domain.usecase.calendar.RefreshMonthEventsUseCase
import com.eddndev.purpura.ui.heatmap.HeatmapCell
import com.eddndev.purpura.ui.heatmap.HeatmapViewModel
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
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

// Maquina de estados del Mapa de calor: rejilla del mes con conteo y nivel por dia, total del mes
// y refresh. `today` se calcula con la misma fecha real que congela el VM (deterministico).
@OptIn(ExperimentalCoroutinesApi::class)
class HeatmapViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeHeatmapEventRepository()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = HeatmapViewModel(
        getMonthEvents = GetMonthEventsUseCase(repository),
        refreshMonthEvents = RefreshMonthEventsUseCase(repository),
    )

    private fun eventOn(date: LocalDate, id: String) = Event(
        id = id,
        userId = "u1",
        type = EventType.junta,
        contact = Contact("Maria", null),
        location = Location(0.0, 0.0, null),
        description = "Evento",
        startsAt = date.atTime(LocalTime.NOON).atZone(zone).toInstant(),
        status = EventStatus.pendiente,
        reminder = Reminder.none,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    @Test
    fun `arranque muestra el mes actual con la celda de hoy`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(YearMonth.from(today), state.yearMonth)
        assertFalse(state.isLoading)
        assertTrue(state.cells.isNotEmpty())
        val todayCell = state.cells.filterIsInstance<HeatmapCell.Day>().first { it.date == today }
        assertTrue(todayCell.isToday)
    }

    @Test
    fun `cuenta los eventos por dia, satura el nivel y suma el total del mes`() = runTest(dispatcher) {
        repository.monthFlow.value = listOf(
            eventOn(today, "a"),
            eventOn(today, "b"),
            eventOn(today, "c"),
            eventOn(today, "d"),
            eventOn(today, "e"),
        )
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        val todayCell = state.cells.filterIsInstance<HeatmapCell.Day>().first { it.date == today }
        assertEquals(5, todayCell.count)
        assertEquals(4, todayCell.level) // 5 eventos saturan el nivel maximo
        assertEquals(5, state.totalEvents)
    }

    @Test
    fun `el mes siguiente sincroniza sus bounds`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.nextMonth()

        val next = YearMonth.from(today).plusMonths(1)
        assertEquals(next, viewModel.uiState.value.yearMonth)
        assertEquals(next.atDay(1) to next.atEndOfMonth(), repository.refreshRanges.last())
    }

    @Test
    fun `un refresh fallido conserva la rejilla y emite un aviso de un solo uso`() = runTest(dispatcher) {
        repository.monthFlow.value = listOf(eventOn(today, "a"))
        repository.refreshError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state.cells.isNotEmpty())
        assertNotNull(state.errorRes)
        assertFalse(state.isLoading)

        viewModel.errorShown()
        assertNull(viewModel.uiState.value.errorRes)
    }
}

private class FakeHeatmapEventRepository : EventRepository {
    val monthFlow = MutableStateFlow<List<Event>>(emptyList())
    val refreshRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var refreshError: Throwable? = null

    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> = monthFlow

    override suspend fun refreshRange(from: LocalDate, to: LocalDate) {
        refreshRanges += from to to
        refreshError?.let { throw it }
    }

    override fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>> = notUsed()
    override suspend fun query(query: EventQuery): Page<Event> = notUsed()
    override suspend fun getById(id: String): Event = notUsed()
    override suspend fun create(draft: NewEventDraft): Event = notUsed()
    override suspend fun update(id: String, patch: EventPatch): Event = notUsed()
    override suspend fun changeStatus(id: String, status: EventStatus): Event = notUsed()
    override suspend fun delete(id: String) = notUsed()
    override suspend fun export(query: EventQuery?): ExportDocument = notUsed()
    override suspend fun import(request: ImportRequest): ImportResult = notUsed()

    private fun notUsed(): Nothing = error("no usado en HeatmapViewModelTest")
}
