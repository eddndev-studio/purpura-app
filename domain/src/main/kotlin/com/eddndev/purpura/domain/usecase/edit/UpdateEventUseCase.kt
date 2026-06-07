package com.eddndev.purpura.domain.usecase.edit

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import com.eddndev.purpura.domain.repository.sync
import javax.inject.Inject

// REQ-QUERY-011/012 (+ descripcion/tipo/recordatorio/fecha). Tras actualizar, sincroniza el
// recordatorio con los nuevos datos (pudo cambiar startsAt, reminder o el estatus); sync cancela
// si el evento quedo realizado.
class UpdateEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String, patch: EventPatch): Event {
        if (patch.isEmpty) throw DomainError.Validation()
        val updated = repository.update(id, patch)
        reminderScheduler.sync(updated)
        return updated
    }
}
