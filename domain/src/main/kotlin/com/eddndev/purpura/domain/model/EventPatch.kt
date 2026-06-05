package com.eddndev.purpura.domain.model

import java.time.Instant

// Cambios parciales de un evento (PATCH /events/{id}, contrato §5.8). Solo los campos
// no nulos se aplican; los nulos se conservan. El estatus NO se cambia aqui (tiene su
// propio endpoint). Al menos un campo debe ir presente.
data class EventPatch(
    val type: EventType? = null,
    val contact: Contact? = null,
    val location: Location? = null,
    val description: String? = null,
    val startsAt: Instant? = null,
    val reminder: Reminder? = null,
) {
    val isEmpty: Boolean
        get() = type == null && contact == null && location == null &&
            description == null && startsAt == null && reminder == null
}
