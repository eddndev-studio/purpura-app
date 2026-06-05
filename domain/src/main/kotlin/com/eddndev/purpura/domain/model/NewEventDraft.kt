package com.eddndev.purpura.domain.model

import java.time.Instant

// Datos de creacion de un evento (sin id, status ni timestamps). El backend asigna id y
// timestamps; el estatus inicial siempre es `pendiente` (contrato §5.5). El estatus
// elegido en REQ-ADD-007 se aplica en un segundo paso (AddEventUseCase).
data class NewEventDraft(
    val type: EventType,
    val contact: Contact,
    val location: Location,
    val description: String,
    val startsAt: Instant,
    val reminder: Reminder,
)
