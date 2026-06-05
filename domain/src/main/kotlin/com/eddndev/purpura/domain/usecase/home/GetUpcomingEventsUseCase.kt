package com.eddndev.purpura.domain.usecase.home

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

// REQ-HOME-001/002, REQ-NOTIF-002. Observa los eventos de la ventana [hoy, hoy+4] (5 dias
// inclusive). El refresh contra la API ocurre dentro del repositorio (politica offline §9).
class GetUpcomingEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    operator fun invoke(today: LocalDate, daysAhead: Int = DAYS_AHEAD): Flow<List<Event>> =
        repository.observeUpcoming(today, daysAhead)

    companion object {
        const val DAYS_AHEAD = 4
    }
}
