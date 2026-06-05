package com.eddndev.purpura.domain.usecase.edit

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-QUERY-011/012 (+ descripcion/tipo/recordatorio/fecha). Tras actualizar, reprograma el
// recordatorio con los nuevos datos (puede haber cambiado startsAt o reminder).
class UpdateEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String, patch: EventPatch): Event {
        if (patch.isEmpty) throw DomainError.Validation()
        val updated = repository.update(id, patch)
        reminderScheduler.schedule(updated)
        return updated
    }
}
