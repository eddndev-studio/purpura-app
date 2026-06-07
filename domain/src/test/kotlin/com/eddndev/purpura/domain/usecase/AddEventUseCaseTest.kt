package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.support.FakeReminderScheduler
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.add.AddEventUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddEventUseCaseTest {

    private val repository = FakeEventRepository()
    private val scheduler = FakeReminderScheduler()
    private val useCase = AddEventUseCase(repository, scheduler)

    @Test
    fun `estatus pendiente crea una vez sin cambiar estatus y programa recordatorio`() = runTest {
        val created = TestData.event(status = EventStatus.pendiente)
        repository.createResult = created

        val result = useCase(TestData.draft, EventStatus.pendiente)

        assertEquals(created, result)
        assertEquals(1, repository.createdDrafts.size)
        assertTrue(repository.statusChanges.isEmpty())
        assertEquals(listOf(created), scheduler.scheduled)
    }

    @Test
    fun `crear como realizado aplica el segundo paso y cancela el recordatorio en vez de programarlo`() = runTest {
        val created = TestData.event(id = "e1", status = EventStatus.pendiente)
        val done = created.copy(status = EventStatus.realizado)
        repository.createResult = created
        repository.changeStatusResult = done

        val result = useCase(TestData.draft, EventStatus.realizado)

        assertEquals(done, result)
        assertEquals(listOf("e1" to EventStatus.realizado), repository.statusChanges)
        assertTrue(scheduler.scheduled.isEmpty())
        assertEquals(listOf("e1"), scheduler.cancelled)
    }

    @Test
    fun `crear como aplazado programa el recordatorio del evento final`() = runTest {
        val created = TestData.event(id = "e1", status = EventStatus.pendiente)
        val postponed = created.copy(status = EventStatus.aplazado)
        repository.createResult = created
        repository.changeStatusResult = postponed

        val result = useCase(TestData.draft, EventStatus.aplazado)

        assertEquals(postponed, result)
        assertEquals(listOf(postponed), scheduler.scheduled)
        assertTrue(scheduler.cancelled.isEmpty())
    }
}
