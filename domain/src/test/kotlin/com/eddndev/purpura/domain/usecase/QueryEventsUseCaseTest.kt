package com.eddndev.purpura.domain.usecase

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.query.Pagination
import com.eddndev.purpura.domain.support.FakeEventRepository
import com.eddndev.purpura.domain.support.TestData
import com.eddndev.purpura.domain.usecase.query.QueryEventsUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QueryEventsUseCaseTest {

    private val repository = FakeEventRepository()
    private val useCase = QueryEventsUseCase(repository)

    private fun page() = Page(
        items = listOf(TestData.event()),
        pagination = Pagination(1, 20, 1, 1, "startsAt:asc"),
    )

    @Test
    fun `rango con from posterior a to lanza Validation y no consulta`() = runTest {
        val query = EventQuery(
            mode = QueryMode.por_rango,
            from = LocalDate.of(2026, 6, 10),
            to = LocalDate.of(2026, 6, 5),
        )

        val error = runCatching { useCase(query) }.exceptionOrNull()

        assertTrue(error is DomainError.Validation)
        assertEquals(0, repository.queryCallCount)
    }

    @Test
    fun `rango valido consulta al repositorio`() = runTest {
        repository.queryResult = page()
        val query = EventQuery(
            mode = QueryMode.por_rango,
            from = LocalDate.of(2026, 6, 5),
            to = LocalDate.of(2026, 6, 10),
        )

        val result = useCase(query)

        assertEquals(1, repository.queryCallCount)
        assertEquals(query, repository.lastQuery)
        assertEquals(1, result.items.size)
    }

    @Test
    fun `sin modo consulta al repositorio sin validar rango`() = runTest {
        repository.queryResult = page()

        useCase(EventQuery(mode = null))

        assertEquals(1, repository.queryCallCount)
    }
}
