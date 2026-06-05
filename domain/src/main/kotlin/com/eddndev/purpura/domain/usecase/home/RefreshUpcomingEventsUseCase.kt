package com.eddndev.purpura.domain.usecase.home

import com.eddndev.purpura.domain.repository.EventRepository
import java.time.LocalDate
import javax.inject.Inject

// REQ-HOME-001, politica offline §9. `observeUpcoming` solo lee del cache (Room); este caso de
// uso dispara la sincronizacion contra la API para la MISMA ventana [hoy, hoy+daysAhead] que
// observa Inicio. Usar el mismo `daysAhead` por defecto evita que los ultimos dias queden vacios.
class RefreshUpcomingEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(today: LocalDate, daysAhead: Int = GetUpcomingEventsUseCase.DAYS_AHEAD) {
        repository.refreshRange(from = today, to = today.plusDays(daysAhead.toLong()))
    }
}
