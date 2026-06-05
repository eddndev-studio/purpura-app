package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.Event

// Puerto del programador de notificaciones locales (REQ-NOTIF-001). El instante de disparo
// se calcula desde `reminder` + `startsAt`: none no programa, at_time = startsAt,
// ten_minutes_before = startsAt-10m, one_day_before = startsAt-24h. La implementacion
// (AlarmManager) vive en data; el backend solo persiste los campos (contrato §7).
interface ReminderScheduler {
    fun schedule(event: Event)
    fun cancel(eventId: String)
}
