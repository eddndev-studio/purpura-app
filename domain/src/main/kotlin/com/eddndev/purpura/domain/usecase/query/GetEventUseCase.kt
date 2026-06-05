package com.eddndev.purpura.domain.usecase.query

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.EventRepository
import javax.inject.Inject

// REQ-QUERY-007..009, REQ-CAL-002. Obtiene un evento por id (solo del propietario).
class GetEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(id: String): Event = repository.getById(id)
}
