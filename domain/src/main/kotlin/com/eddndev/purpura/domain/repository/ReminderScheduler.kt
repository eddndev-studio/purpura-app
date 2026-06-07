package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus

// Puerto del programador de notificaciones locales (REQ-NOTIF-001). El instante de disparo
// se calcula desde `reminder` + `startsAt`: none no programa, at_time = startsAt,
// ten_minutes_before = startsAt-10m, one_day_before = startsAt-24h. La implementacion
// (AlarmManager) vive en data; el backend solo persiste los campos (contrato §7).
interface ReminderScheduler {
    fun schedule(event: Event)
    fun cancel(eventId: String)
}

// Regla de negocio unica: un evento `realizado` no necesita recordatorio (se cancela); cualquier
// otro estatus lo (re)programa segun sus datos (schedule respeta reminder=none y disparos vencidos).
// Todos los casos de uso que tocan un evento (crear, editar, cambiar estatus) deben pasar por aqui
// para que la regla sea consistente sin importar el camino. Vive en dominio para ser testeable con
// un ReminderScheduler de prueba.
fun ReminderScheduler.sync(event: Event) {
    if (event.status == EventStatus.realizado) {
        cancel(event.id)
    } else {
        schedule(event)
    }
}
