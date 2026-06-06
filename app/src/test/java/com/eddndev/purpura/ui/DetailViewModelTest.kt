package com.eddndev.purpura.ui

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.usecase.edit.ChangeEventStatusUseCase
import com.eddndev.purpura.domain.usecase.edit.DeleteEventUseCase
import com.eddndev.purpura.domain.usecase.query.GetEventUseCase
import com.eddndev.purpura.ui.detail.DetailViewModel
import com.eddndev.purpura.ui.support.FakeEventRepository
import com.eddndev.purpura.ui.support.FakeReminderScheduler
import com.eddndev.purpura.ui.support.sampleEvent
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

// Prueba la maquina de estados de DetailViewModel: carga por id, cambio de estatus y eliminacion,
// con sus rutas de error que conservan el evento. UnconfinedTestDispatcher para correr los launch
// de inmediato.
@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()
    private val scheduler = FakeReminderScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = DetailViewModel(
        getEvent = GetEventUseCase(repository),
        changeEventStatus = ChangeEventStatusUseCase(repository, scheduler),
        deleteEvent = DeleteEventUseCase(repository, scheduler),
    )

    @Test
    fun `cargar un evento lo muestra`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.load("e1")

        val state = viewModel.uiState.value
        assertEquals("e1", state.event?.id)
        assertFalse(state.isLoading)
        assertNull(state.errorRes)
        assertEquals(listOf("e1"), repository.getByIdCalls)
    }

    @Test
    fun `un error al cargar emite aviso y no deja evento`() = runTest(dispatcher) {
        repository.getByIdError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.load("e1")

        val state = viewModel.uiState.value
        assertNull(state.event)
        assertFalse(state.isLoading)
        assertTrue(state.loadFailed)
        assertNotNull(state.errorRes)
    }

    @Test
    fun `retry reintenta la carga tras un fallo`() = runTest(dispatcher) {
        repository.getByIdError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.load("e1")
        assertTrue(viewModel.uiState.value.loadFailed)
        assertEquals(listOf("e1"), repository.getByIdCalls)

        repository.getByIdError = null
        repository.event = sampleEvent("e1")
        viewModel.retry()

        val state = viewModel.uiState.value
        assertEquals("e1", state.event?.id)
        assertFalse(state.loadFailed)
        assertEquals(listOf("e1", "e1"), repository.getByIdCalls)
    }

    @Test
    fun `cambiar estatus actualiza el evento`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1", status = EventStatus.pendiente)
        repository.changeStatusResult = sampleEvent("e1", status = EventStatus.realizado)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.load("e1")

        viewModel.changeStatus(EventStatus.realizado)

        assertEquals(EventStatus.realizado, viewModel.uiState.value.event?.status)
        assertEquals(listOf("e1" to EventStatus.realizado), repository.statusChanges)
    }

    @Test
    fun `un error al cambiar estatus conserva el evento`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1", status = EventStatus.pendiente)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.load("e1")

        repository.changeStatusError = DomainError.Network
        viewModel.changeStatus(EventStatus.realizado)

        val state = viewModel.uiState.value
        assertEquals(EventStatus.pendiente, state.event?.status)
        assertNotNull(state.errorRes)
        assertFalse(state.isWorking)

        // El aviso es de un solo uso: errorShown() lo limpia sin tocar el evento.
        viewModel.errorShown()
        assertNull(viewModel.uiState.value.errorRes)
        assertEquals(EventStatus.pendiente, viewModel.uiState.value.event?.status)
    }

    @Test
    fun `eliminar marca deleted y cancela el recordatorio`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.load("e1")

        viewModel.delete()

        assertTrue(viewModel.uiState.value.deleted)
        assertEquals(listOf("e1"), repository.deletedIds)
        assertEquals(listOf("e1"), scheduler.cancelled)
    }

    @Test
    fun `un error al eliminar conserva el evento`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.load("e1")

        repository.deleteError = DomainError.Network
        viewModel.delete()

        val state = viewModel.uiState.value
        assertFalse(state.deleted)
        assertNotNull(state.event)
        assertNotNull(state.errorRes)
    }

    @Test
    fun `load es idempotente por id`() = runTest(dispatcher) {
        repository.event = sampleEvent("e1")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.load("e1")
        viewModel.load("e1")

        assertEquals(listOf("e1"), repository.getByIdCalls)
    }
}
