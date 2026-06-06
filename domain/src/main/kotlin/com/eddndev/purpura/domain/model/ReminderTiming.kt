package com.eddndev.purpura.domain.model

import java.time.Duration
import java.time.Instant

// REQ-NOTIF-001. Calcula el instante de disparo de la notificacion local a partir del recordatorio
// y la hora de inicio del evento: none no programa (null), at_time = startsAt, ten_minutes_before =
// startsAt - 10m, one_day_before = startsAt - 24h. Es logica pura del dominio (sin AlarmManager), de
// modo que la implementacion del programador (capa data) solo traduce este instante a una alarma y
// esta regla queda cubierta por pruebas sin tocar el framework de Android.
fun Reminder.triggerAt(startsAt: Instant): Instant? = when (this) {
    Reminder.none -> null
    Reminder.at_time -> startsAt
    Reminder.ten_minutes_before -> startsAt.minus(Duration.ofMinutes(10))
    Reminder.one_day_before -> startsAt.minus(Duration.ofDays(1))
}
