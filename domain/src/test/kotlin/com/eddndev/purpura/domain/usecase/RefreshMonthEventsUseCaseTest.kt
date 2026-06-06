package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.usecase.calendar.RefreshMonthEventsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RefreshMonthEventsUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = RefreshMonthEventsUseCase(repository)

    // Bounds del mes con literales (NO la misma formula que el caso de uso) para fijar el pareo
    // EXACTO con observeMonth y atrapar un off-by-one. Febrero 2026 tiene 28 dias.
    @Test
    fun `sincroniza desde el primer hasta el ultimo dia del mes`() = runTest {
        useCase(year = 2026, month = 2)

        assertEquals(1, repository.refreshRangeCount)
        assertEquals(
            LocalDate.of(2026, 2, 1) to LocalDate.of(2026, 2, 28),
            repository.lastRefreshRange,
        )
    }

    // Un mes de 31 dias confirma que el limite superior no se queda corto.
    @Test
    fun `cubre meses de 31 dias`() = runTest {
        useCase(year = 2026, month = 1)

        assertEquals(
            LocalDate.of(2026, 1, 1) to LocalDate.of(2026, 1, 31),
            repository.lastRefreshRange,
        )
    }
}
