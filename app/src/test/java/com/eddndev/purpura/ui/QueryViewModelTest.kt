package com.eddndev.purpura.ui

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.domain.usecase.query.QueryEventsUseCase
import com.eddndev.purpura.ui.query.QueryFilters
import com.eddndev.purpura.ui.query.QueryViewModel
import com.eddndev.purpura.ui.support.FakeEventRepository
import com.eddndev.purpura.ui.support.FakeEventRepository.Companion.emptyPage
import com.eddndev.purpura.ui.support.FakeEventRepository.Companion.pageOf
import com.eddndev.purpura.ui.support.sampleEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.time.LocalDate

// Prueba la maquina de estados de QueryViewModel: busqueda con filtros -> EventQuery, paginacion
// acumulativa, error que conserva la lista y validacion de rango. UnconfinedTestDispatcher para
// que el search del init corra de inmediato.
@OptIn(ExperimentalCoroutinesApi::class)
class QueryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = QueryViewModel(QueryEventsUseCase(repository))

    @Test
    fun `la busqueda inicial puebla la lista`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 1))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(listOf("a"), state.events.map { it.id })
        assertFalse(state.isLoading)
        assertTrue(state.hasSearched)
        assertFalse(state.canLoadMore)
        assertFalse(state.isEmpty)
        assertNull(state.errorRes)
    }

    @Test
    fun `aplicar filtros construye el EventQuery y reemplaza la lista`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 1))
        repository.pages.addLast(pageOf(listOf(sampleEvent("b")), page = 1, totalPages = 1))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.search(QueryFilters(type = EventType.examen, status = EventStatus.pendiente))

        val query = repository.queries.last()
        assertEquals(EventType.examen, query.type)
        assertEquals(EventStatus.pendiente, query.status)
        assertEquals(1, query.page)
        assertEquals(listOf("b"), viewModel.uiState.value.events.map { it.id })
    }

    @Test
    fun `loadMore acumula la siguiente pagina y respeta canLoadMore`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 2))
        repository.pages.addLast(pageOf(listOf(sampleEvent("b")), page = 2, totalPages = 2))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        assertTrue(viewModel.uiState.value.canLoadMore)

        viewModel.loadMore()

        assertEquals(2, repository.queries.last().page)
        assertEquals(listOf("a", "b"), viewModel.uiState.value.events.map { it.id })
        assertFalse(viewModel.uiState.value.canLoadMore)
    }

    @Test
    fun `un error en la busqueda conserva la lista y emite un aviso`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 1))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        repository.queryError = DomainError.Network
        viewModel.search(QueryFilters(type = EventType.examen))

        val state = viewModel.uiState.value
        assertEquals(listOf("a"), state.events.map { it.id })
        assertNotNull(state.errorRes)
        assertFalse(state.isLoading)

        // El aviso es de un solo uso: errorShown() lo limpia.
        viewModel.errorShown()
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `loadMore en la ultima pagina no consulta de nuevo`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 1))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        assertFalse(viewModel.uiState.value.canLoadMore)
        val queriesAfterInit = repository.queries.size

        viewModel.loadMore()

        assertEquals(queriesAfterInit, repository.queries.size)
    }

    @Test
    fun `un error al paginar conserva la lista, no avanza la pagina y permite reintentar`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 2))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        repository.queryError = DomainError.Network
        viewModel.loadMore()

        val afterError = viewModel.uiState.value
        assertEquals(listOf("a"), afterError.events.map { it.id })
        assertNotNull(afterError.errorRes)
        assertFalse(afterError.isPaging)
        assertTrue(afterError.canLoadMore)

        // Como la pagina no avanzo, un reintento vuelve a pedir la pagina 2 (no la 3).
        repository.queryError = null
        repository.pages.addLast(pageOf(listOf(sampleEvent("b")), page = 2, totalPages = 2))
        viewModel.loadMore()
        assertEquals(2, repository.queries.last().page)
        assertEquals(listOf("a", "b"), viewModel.uiState.value.events.map { it.id })
    }

    @Test
    fun `una segunda llamada a loadMore en vuelo se ignora (guard isPaging)`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 3))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val queriesAfterInit = repository.queries.size

        val gate = CompletableDeferred<Unit>()
        repository.queryGate = gate
        viewModel.loadMore() // pagina 2: queda suspendida en el gate (isPaging=true)
        viewModel.loadMore() // debe early-return por el guard isPaging

        assertEquals(queriesAfterInit + 1, repository.queries.size)
        gate.complete(Unit)
    }

    @Test
    fun `sin resultados muestra el estado vacio`() = runTest(dispatcher) {
        repository.pages.addLast(emptyPage())
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertTrue(state.events.isEmpty())
        assertTrue(state.hasSearched)
        assertTrue(state.isEmpty)
    }

    @Test
    fun `un rango invalido emite validacion sin consultar`() = runTest(dispatcher) {
        repository.pages.addLast(pageOf(listOf(sampleEvent("a")), page = 1, totalPages = 1))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val queriesBefore = repository.queries.size

        viewModel.search(
            QueryFilters(
                mode = QueryMode.por_rango,
                from = LocalDate.of(2026, 6, 10),
                to = LocalDate.of(2026, 6, 1),
            ),
        )

        assertNotNull(viewModel.uiState.value.errorRes)
        assertEquals(queriesBefore, repository.queries.size)
    }
}
