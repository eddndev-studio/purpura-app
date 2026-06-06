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
import com.eddndev.purpura.ui.calendar.CalendarCell
import com.eddndev.purpura.ui.calendar.CalendarViewModel
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

// Maquina de estados del Calendario: rejilla del mes, agrupacion por dia, seleccion y refresh.
// `today` se calcula con la misma fecha real que congela el VM, asi los asserts son deterministicos
// sin inyectar un reloj (mismo enfoque que HomeViewModel/HomeViewModelTest).
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeMonthEventRepository()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = CalendarViewModel(
        getMonthEvents = GetMonthEventsUseCase(repository),
        refreshMonthEvents = RefreshMonthEventsUseCase(repository),
    )

    private fun eventOn(date: LocalDate, id: String, type: EventType = EventType.junta) = Event(
        id = id,
        userId = "u1",
        type = type,
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
    fun `arranque muestra el mes actual con hoy seleccionado`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(YearMonth.from(today), state.yearMonth)
        assertEquals(today, state.selectedDate)
        assertFalse(state.isLoading)
        assertTrue(state.cells.isNotEmpty())
        val todayCell = state.cells.filterIsInstance<CalendarCell.Day>().first { it.date == today }
        assertTrue(todayCell.isToday)
        assertTrue(todayCell.isSelected)
    }

    @Test
    fun `agrupa los eventos del dia en puntos por tipo y en la lista`() = runTest(dispatcher) {
        repository.monthFlow.value = listOf(
            eventOn(today, "a", EventType.junta),
            eventOn(today, "b", EventType.examen),
            eventOn(today, "c", EventType.junta),
        )
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        val todayCell = state.cells.filterIsInstance<CalendarCell.Day>().first { it.date == today }
        assertEquals(3, todayCell.eventCount)
        assertEquals(listOf(EventType.junta, EventType.examen), todayCell.typeDots) // distintos, en orden
        assertEquals(3, state.selectedDayEvents.size) // hoy queda seleccionado por defecto
    }

    @Test
    fun `seleccionar un dia sin eventos vacia la lista inferior`() = runTest(dispatcher) {
        repository.monthFlow.value = listOf(eventOn(today, "a"))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.selectDate(today.plusDays(1))

        val state = viewModel.uiState.value
        assertEquals(today.plusDays(1), state.selectedDate)
        assertTrue(state.selectedDayEvents.isEmpty())
    }

    @Test
    fun `el mes siguiente sincroniza sus bounds y reinicia la seleccion`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.nextMonth()

        val next = YearMonth.from(today).plusMonths(1)
        assertEquals(next, viewModel.uiState.value.yearMonth)
        assertEquals(next.atDay(1) to next.atEndOfMonth(), repository.refreshRanges.last())
        assertEquals(next.atDay(1), viewModel.uiState.value.selectedDate) // sin hoy ni eventos -> dia 1
    }

    // Navegar de mes cancela el refresh anterior; esa cancelacion NO debe pintar un aviso de error
    // (toErrorMessageRes re-lanza CancellationException). Aqui el refresh inicial queda suspendido
    // en un gate y nextMonth lo cancela.
    @Test
    fun `cancelar el refresh al navegar de mes no surface un error`() = runTest(dispatcher) {
        repository.gate = CompletableDeferred()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.nextMonth()

        assertNull(viewModel.uiState.value.errorRes)
        repository.gate?.complete(Unit) // libera el refresh en vuelo
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

// Fake reactivo del puerto: observeMonth emite monthFlow; refreshRange registra los bounds y puede
// fallar. Las demas operaciones no se usan en este slice y fallan ruidosamente.
private class FakeMonthEventRepository : EventRepository {
    val monthFlow = MutableStateFlow<List<Event>>(emptyList())
    val refreshRanges = mutableListOf<Pair<LocalDate, LocalDate>>()
    var refreshError: Throwable? = null
    var gate: CompletableDeferred<Unit>? = null

    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> = monthFlow

    override suspend fun refreshRange(from: LocalDate, to: LocalDate) {
        refreshRanges += from to to
        gate?.await()
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

    private fun notUsed(): Nothing = error("no usado en CalendarViewModelTest")
}
