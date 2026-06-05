package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.support.FakeReminderScheduler
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.edit.ChangeEventStatusUseCase
import com.eddndev.purpura.domain.usecase.edit.DeleteEventUseCase
import com.eddndev.purpura.domain.usecase.edit.UpdateEventUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditEventUseCasesTest {

    private val repository = FakeEventRepository()
    private val scheduler = FakeReminderScheduler()

    @Test
    fun `cambiar estatus llama al repositorio y reprograma`() = runTest {
        val updated = TestData.event(status = EventStatus.aplazado)
        repository.changeStatusResult = updated
        val useCase = ChangeEventStatusUseCase(repository, scheduler)

        val result = useCase("e1", EventStatus.aplazado)

        assertEquals(updated, result)
        assertEquals(listOf("e1" to EventStatus.aplazado), repository.statusChanges)
        assertEquals(listOf(updated), scheduler.scheduled)
    }

    @Test
    fun `eliminar borra y cancela el recordatorio`() = runTest {
        val useCase = DeleteEventUseCase(repository, scheduler)

        useCase("e1")

        assertEquals(listOf("e1"), repository.deletedIds)
        assertEquals(listOf("e1"), scheduler.cancelled)
    }

    @Test
    fun `actualizar con patch vacio lanza Validation y no toca el repositorio`() = runTest {
        val useCase = UpdateEventUseCase(repository, scheduler)

        val error = runCatching { useCase("e1", EventPatch()) }.exceptionOrNull()

        assertTrue(error is DomainError.Validation)
        assertTrue(repository.patches.isEmpty())
        assertTrue(scheduler.scheduled.isEmpty())
    }

    @Test
    fun `actualizar con patch valido aplica y reprograma`() = runTest {
        val updated = TestData.event()
        repository.updateResult = updated
        val patch = EventPatch(description = "Nueva descripcion")
        val useCase = UpdateEventUseCase(repository, scheduler)

        val result = useCase("e1", patch)

        assertEquals(updated, result)
        assertEquals(listOf("e1" to patch), repository.patches)
        assertEquals(listOf(updated), scheduler.scheduled)
    }
}
