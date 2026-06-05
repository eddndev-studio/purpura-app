package com.eddndev.purpura.domain.usecase.add

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-ADD-001..009. Doble paso (06-app-architecture §4.3): POST /events crea siempre con
// status=pendiente (contrato §5.5); si el estatus elegido en REQ-ADD-007 no es pendiente,
// se aplica con PATCH /events/{id}/status. Al confirmar, programa el recordatorio local
// (REQ-NOTIF-001; schedule respeta reminder=none = no programa).
class AddEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(draft: NewEventDraft, chosenStatus: EventStatus): Event {
        val created = repository.create(draft)
        val event = if (chosenStatus != EventStatus.pendiente) {
            repository.changeStatus(created.id, chosenStatus)
        } else {
            created
        }
        reminderScheduler.schedule(event)
        return event
    }
}
