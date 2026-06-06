package com.eddndev.purpura.domain.usecase.calendar

import com.eddndev.purpura.domain.repository.EventRepository
import java.time.LocalDate
import javax.inject.Inject

// REQ-CAL-001 + heatmap, politica offline §9. `observeMonth` solo LEE del cache (Room); este caso
// de uso dispara la sincronizacion contra la API para la MISMA ventana [primer dia, ultimo dia]
// del mes que observa el calendario/heatmap, de modo que el cache no quede vacio.
//
// Los bounds deben coincidir EXACTO con EventRepositoryImpl.observeMonth
// (primero .. primero.plusMonths(1).minusDays(1)); si se quedaran cortos, los ultimos dias del
// mes nunca se poblarian.
//
// TODO(#8): refreshRange pide una sola pagina (pageSize=100, sin paginar). Un mes con mas de 100
// eventos solo cachearia los primeros 100 -> el calendario/heatmap subcontaria. Improbable a la
// escala actual; si se vuelve real, paginar leyendo response.pagination.
class RefreshMonthEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(year: Int, month: Int) {
        val first = LocalDate.of(year, month, 1)
        repository.refreshRange(from = first, to = first.plusMonths(1).minusDays(1))
    }
}
