package com.eddndev.purpura.domain.model

import com.eddndev.purpura.domain.error.DomainError
import java.time.Instant

// Agregado raiz. La factory `create` valida las invariantes (03-domain-entities §6.1)
// ANTES de tocar la red: descripcion no vacia (trim), ubicacion en rango, estatus
// inicial forzado a `pendiente`. El tipo y el recordatorio quedan garantizados por
// ser enums. El cambio de estatus posterior es libre entre los tres valores.
data class Event(
    val id: String,
    val userId: String,
    val type: EventType,
    val contact: Contact,
    val location: Location,
    val description: String,
    val startsAt: Instant,
    val status: EventStatus,
    val reminder: Reminder,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        private val LAT_RANGE = -90.0..90.0
        private val LNG_RANGE = -180.0..180.0

        fun create(
            id: String,
            userId: String,
            type: EventType,
            contact: Contact,
            location: Location,
            description: String,
            startsAt: Instant,
            reminder: Reminder,
            createdAt: Instant,
            updatedAt: Instant = createdAt,
        ): Event {
            val cleanDescription = description.trim()
            if (cleanDescription.isEmpty()) throw DomainError.EmptyDescription
            if (location.lat !in LAT_RANGE || location.lng !in LNG_RANGE) throw DomainError.InvalidLocation
            return Event(
                id = id,
                userId = userId,
                type = type,
                contact = contact,
                location = location,
                description = cleanDescription,
                startsAt = startsAt,
                status = EventStatus.pendiente,
                reminder = reminder,
                createdAt = createdAt,
                updatedAt = updatedAt,
            )
        }
    }
}
