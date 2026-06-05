package com.eddndev.purpura.domain.usecase.calendar

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

// REQ-CAL-001/003 + heatmap. Observa los eventos del mes para pintar puntos en el
// calendario y agregar el conteo por dia del heatmap.
class GetMonthEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    operator fun invoke(year: Int, month: Int): Flow<List<Event>> =
        repository.observeMonth(year, month)
}
