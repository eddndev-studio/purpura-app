package com.eddndev.purpura.domain.usecase.edit

import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-QUERY-013. Tras eliminar, cancela cualquier recordatorio pendiente del evento.
class DeleteEventUseCase @Inject constructor(
    private val repository: EventRepository,
    private val reminderScheduler: ReminderScheduler,
) {
    suspend operator fun invoke(id: String) {
        repository.delete(id)
        reminderScheduler.cancel(id)
    }
}
