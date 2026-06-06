package com.eddndev.purpura.ui

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.domain.usecase.add.AddEventUseCase
import com.eddndev.purpura.ui.addevent.AddEventInput
import com.eddndev.purpura.ui.addevent.AddEventViewModel
import com.eddndev.purpura.ui.support.FakeEventRepository
import com.eddndev.purpura.ui.support.FakeReminderScheduler
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
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

// Prueba la maquina de estados de AddEventViewModel: validacion por campo, construccion del draft
// (incluido el startsAt en la zona del dispositivo), paso del estatus elegido y la ruta de error.
@OptIn(ExperimentalCoroutinesApi::class)
class AddEventViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()
    private val scheduler = FakeReminderScheduler()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = AddEventViewModel(
        addEvent = AddEventUseCase(repository, scheduler),
    )

    private fun validInput(
        description: String = "Revision de avance",
        contactName: String = "Maria",
        placeLabel: String = "  Campus Sur  ",
        type: EventType = EventType.junta,
        status: EventStatus = EventStatus.pendiente,
        reminder: Reminder = Reminder.ten_minutes_before,
        date: LocalDate? = LocalDate.of(2026, 6, 10),
        time: LocalTime? = LocalTime.of(15, 30),
    ) = AddEventInput(description, contactName, placeLabel, type, status, reminder, date, time)

    @Test
    fun `un envio valido crea el evento y marca saved`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo", status = EventStatus.pendiente)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput())

        val draft = repository.createdDrafts.single()
        assertEquals(EventType.junta, draft.type)
        assertEquals("Maria", draft.contact.name)
        assertEquals("Campus Sur", draft.location.label) // etiqueta recortada
        assertEquals(0.0, draft.location.lat, 0.0)        // lat/lng placeholder hasta el mapa
        assertEquals(Reminder.ten_minutes_before, draft.reminder)
        assertTrue(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertEquals(listOf(sampleEvent("nuevo")), scheduler.scheduled) // programa el recordatorio
    }

    @Test
    fun `el startsAt se calcula en la zona del dispositivo`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }
        val date = LocalDate.of(2026, 6, 10)
        val time = LocalTime.of(15, 30)

        viewModel.submit(validInput(date = date, time = time))

        val expected = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant()
        assertEquals(expected, repository.createdDrafts.single().startsAt)
    }

    @Test
    fun `un estatus distinto de pendiente se aplica en el segundo paso`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo", status = EventStatus.pendiente)
        repository.changeStatusResult = sampleEvent("nuevo", status = EventStatus.realizado)
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput(status = EventStatus.realizado))

        assertEquals(listOf("nuevo" to EventStatus.realizado), repository.statusChanges)
        assertTrue(viewModel.uiState.value.saved)
    }

    @Test
    fun `una ubicacion en blanco deja la etiqueta nula`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput(placeLabel = "   "))

        assertNull(repository.createdDrafts.single().location.label)
    }

    @Test
    fun `las coordenadas elegidas en el mapa van al draft`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput().copy(lat = 19.5, lng = -99.2))

        val location = repository.createdDrafts.single().location
        assertEquals(19.5, location.lat, 0.0)
        assertEquals(-99.2, location.lng, 0.0)
    }

    @Test
    fun `sin coordenadas la ubicacion queda en cero`() = runTest(dispatcher) {
        repository.createResult = sampleEvent("nuevo")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput()) // lat/lng nulos por defecto

        val location = repository.createdDrafts.single().location
        assertEquals(0.0, location.lat, 0.0)
        assertEquals(0.0, location.lng, 0.0)
    }

    @Test
    fun `una descripcion vacia marca error de campo y no crea`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput(description = "   "))

        assertNotNull(viewModel.uiState.value.descriptionError)
        assertFalse(viewModel.uiState.value.saved)
        assertTrue(repository.createdDrafts.isEmpty())
    }

    @Test
    fun `un contacto vacio marca error de campo`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput(contactName = ""))

        assertNotNull(viewModel.uiState.value.contactError)
        assertTrue(repository.createdDrafts.isEmpty())
    }

    @Test
    fun `sin fecha u hora marca error y no crea`() = runTest(dispatcher) {
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput(date = null))

        assertNotNull(viewModel.uiState.value.dateTimeError)
        assertTrue(repository.createdDrafts.isEmpty())
    }

    @Test
    fun `un fallo de red emite aviso de un solo uso y no marca saved`() = runTest(dispatcher) {
        repository.createError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput())

        assertFalse(viewModel.uiState.value.saved)
        assertFalse(viewModel.uiState.value.isSubmitting)
        assertNotNull(viewModel.uiState.value.errorRes)

        viewModel.errorShown()
        assertNull(viewModel.uiState.value.errorRes)
    }

    @Test
    fun `no reenvia mientras hay un envio en curso`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.createGate = gate
        repository.createResult = sampleEvent("nuevo")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.submit(validInput()) // queda suspendido en el gate
        assertTrue(viewModel.uiState.value.isSubmitting)
        viewModel.submit(validInput()) // ignorado: ya hay un envio en curso

        gate.complete(Unit)
        assertEquals(1, repository.createdDrafts.size)
    }
}
