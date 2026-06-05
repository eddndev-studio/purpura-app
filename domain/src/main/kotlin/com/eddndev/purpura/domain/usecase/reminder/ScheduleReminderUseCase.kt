package com.eddndev.purpura.domain.usecase.reminder

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject

// REQ-NOTIF-001. Programa (o, si reminder=none, omite) la notificacion local de un evento.
// Lo usa el receiver de BOOT_COMPLETED para reprogramar tras reinicio.
class ScheduleReminderUseCase @Inject constructor(
    private val reminderScheduler: ReminderScheduler,
) {
    operator fun invoke(event: Event) = reminderScheduler.schedule(event)
}
