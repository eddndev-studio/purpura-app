package com.eddndev.purpura.domain.usecase.query

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.repository.EventRepository
import javax.inject.Inject

// REQ-QUERY-001..006. Valida `from <= to` para el modo por_rango (REQ-QUERY-003) antes de
// tocar la red, de modo que la validacion local y la del backend hablen el mismo idioma.
class QueryEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(query: EventQuery): Page<Event> {
        if (query.mode == QueryMode.por_rango) {
            val from = query.from
            val to = query.to
            if (from == null || to == null || from.isAfter(to)) {
                throw DomainError.Validation(
                    listOf(DomainError.FieldError("to", "El rango de fechas es inválido.")),
                )
            }
        }
        return repository.query(query)
    }
}
