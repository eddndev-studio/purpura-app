package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.home.GetUpcomingEventsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class GetUpcomingEventsUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = GetUpcomingEventsUseCase(repository)

    @Test
    fun `observa la ventana de hoy mas cuatro dias y emite lo del repositorio`() = runTest {
        val today = LocalDate.of(2026, 6, 5)
        val events = listOf(TestData.event())
        repository.upcomingFlow.value = events

        val emitted = useCase(today).first()

        assertEquals(events, emitted)
        assertEquals(today to 4, repository.lastUpcomingArgs)
    }
}
