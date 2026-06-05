package com.eddndev.purpura.data.local.mapper

import com.eddndev.purpura.data.local.entity.EventEntity
import com.eddndev.purpura.data.mapper.EnumCodec
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.Location
import java.time.Instant
import javax.inject.Inject

// Traduce Event <-> EventEntity (06-app-architecture §6). Instante <-> epochMillis, enums
// <-> codigo ASCII. El dominio nunca importa EventEntity.
class EventEntityMapper @Inject constructor() {

    fun toEntity(event: Event): EventEntity = EventEntity(
        id = event.id,
        userId = event.userId,
        eventType = event.type.name,
        contactName = event.contact.name,
        contactRef = event.contact.ref,
        locationLat = event.location.lat,
        locationLng = event.location.lng,
        locationLabel = event.location.label,
        description = event.description,
        startsAtEpochMs = event.startsAt.toEpochMilli(),
        eventStatus = event.status.name,
        reminderType = event.reminder.name,
        createdAtEpochMs = event.createdAt.toEpochMilli(),
        updatedAtEpochMs = event.updatedAt.toEpochMilli(),
    )

    fun toDomain(entity: EventEntity): Event = Event(
        id = entity.id,
        userId = entity.userId,
        type = EnumCodec.eventType(entity.eventType),
        contact = Contact(entity.contactName, entity.contactRef),
        location = Location(entity.locationLat, entity.locationLng, entity.locationLabel),
        description = entity.description,
        startsAt = Instant.ofEpochMilli(entity.startsAtEpochMs),
        status = EnumCodec.eventStatus(entity.eventStatus),
        reminder = EnumCodec.reminder(entity.reminderType),
        createdAt = Instant.ofEpochMilli(entity.createdAtEpochMs),
        updatedAt = Instant.ofEpochMilli(entity.updatedAtEpochMs),
    )
}
