package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.calendar.GetMonthEventsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetMonthEventsUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = GetMonthEventsUseCase(repository)

    @Test
    fun `observa el flujo de eventos del mes desde el cache`() = runTest {
        val events = listOf(TestData.event("a"), TestData.event("b"))
        repository.monthFlow.value = events

        assertEquals(events, useCase(2026, 6).first())
    }
}
