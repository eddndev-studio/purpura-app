package com.eddndev.purpura.data

import com.eddndev.purpura.data.local.mapper.EventEntityMapper
import com.eddndev.purpura.data.remote.dto.EventDto
import com.eddndev.purpura.data.remote.mapper.EventRemoteMapper
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

// Pruebas de ida y vuelta de los mappers (06-app-architecture §12.1, item 3). Son JVM puras:
// los mappers no tocan APIs de Android.
class EventMapperTest {

    private val remote = EventRemoteMapper()
    private val entity = EventEntityMapper()

    private fun dto() = EventDto(
        id = "a4d2",
        userId = "9b2f",
        eventType = "junta",
        contactName = "Maria",
        contactRef = "",
        locationLat = 19.432608,
        locationLng = -99.133209,
        locationLabel = "",
        description = "Revision",
        startsAt = "2026-06-10T15:30:00Z",
        eventStatus = "realizado",
        reminderType = "ten_minutes_before",
        createdAt = "2026-06-05T12:10:00Z",
        updatedAt = "2026-06-05T13:05:00Z",
    )

    @Test
    fun `dto a dominio normaliza opcionales vacios a null y conserva codigos`() {
        val event = remote.toDomain(dto())

        assertNull(event.contact.ref)
        assertNull(event.location.label)
        assertEquals(EventType.junta, event.type)
        assertEquals(EventStatus.realizado, event.status)
        assertEquals(Reminder.ten_minutes_before, event.reminder)
        assertEquals(Instant.parse("2026-06-10T15:30:00Z"), event.startsAt)
    }

    @Test
    fun `dto a dominio a dto preserva los campos de negocio`() {
        val roundTripped = remote.toDto(remote.toDomain(dto()))
        val original = dto()

        assertEquals(original.eventType, roundTripped.eventType)
        assertEquals(original.eventStatus, roundTripped.eventStatus)
        assertEquals(original.reminderType, roundTripped.reminderType)
        assertEquals(original.startsAt, roundTripped.startsAt)
        assertEquals(original.locationLat, roundTripped.locationLat, 0.0)
        // El opcional vacio "" viaja como null de vuelta (el backend lo normaliza).
        assertNull(roundTripped.contactRef)
    }

    @Test
    fun `dominio a entity a dominio conserva instante y enums`() {
        val event = remote.toDomain(dto())

        val restored = entity.toDomain(entity.toEntity(event))

        assertEquals(event, restored)
        assertEquals(event.startsAt, restored.startsAt)
        assertEquals(event.reminder, restored.reminder)
    }
}
