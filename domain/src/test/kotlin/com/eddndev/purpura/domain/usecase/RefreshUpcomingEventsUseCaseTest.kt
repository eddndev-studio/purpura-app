package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.usecase.home.RefreshUpcomingEventsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefreshUpcomingEventsUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = RefreshUpcomingEventsUseCase(repository)

    @Test
    fun `sincroniza la misma ventana que observa Inicio (hoy mas cuatro dias)`() = runTest {
        val today = LocalDate.of(2026, 6, 5)

        useCase(today)

        assertEquals(1, repository.refreshRangeCount)
        assertEquals(today to today.plusDays(4), repository.lastRefreshRange)
    }
}
