package com.eddndev.purpura.domain.usecase.edit

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-QUERY-010, REQ-ADD-007. Transiciones libres entre los tres estatus. Tras cambiar,
// reprograma el recordatorio segun el evento (schedule respeta reminder=none).
class ChangeEventStatusUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String, status: EventStatus): Event {
        val updated = repository.changeStatus(id, status)
        reminderScheduler.schedule(updated)
        return updated
    }
}
