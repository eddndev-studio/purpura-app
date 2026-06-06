package com.eddndev.purpura.domain.usecase.edit

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-QUERY-010, REQ-ADD-007. Transiciones libres entre los tres estatus. Un evento ya `realizado`
// no necesita recordatorio, asi que se cancela; `pendiente` y `aplazado` lo (re)programan segun el
// evento (schedule respeta reminder=none y descarta disparos vencidos).
class ChangeEventStatusUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String, status: EventStatus): Event {
        val updated = repository.changeStatus(id, status)
        if (status == EventStatus.realizado) {
            reminderScheduler.cancel(id)
        } else {
            reminderScheduler.schedule(updated)
        }
        return updated
    }
}
