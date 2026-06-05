package com.eddndev.purpura.domain

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant

class EventTest {

    private val now: Instant = Instant.parse("2026-06-10T17:00:00Z")

    private fun create(
        description: String = "Reunion de equipo",
        location: Location = Location(19.4326, -99.1332, "CDMX"),
    ): Event = Event.create(
        id = "evt-1",
        userId = "usr-1",
        type = EventType.junta,
        contact = Contact("Ana", null),
        location = location,
        description = description,
        startsAt = now,
        reminder = Reminder.one_day_before,
        createdAt = now,
    )

    @Test
    fun `crea con estatus inicial pendiente`() {
        assertEquals(EventStatus.pendiente, create().status)
    }

    @Test
    fun `recorta la descripcion`() {
        assertEquals("Reunion", create(description = "   Reunion   ").description)
    }

    @Test
    fun `descripcion vacia lanza EmptyDescription`() {
        assertThrows(DomainError.EmptyDescription::class.java) {
            create(description = "    ")
        }
    }

    @Test
    fun `latitud fuera de rango lanza InvalidLocation`() {
        assertThrows(DomainError.InvalidLocation::class.java) {
            create(location = Location(200.0, 0.0, null))
        }
    }

    @Test
    fun `longitud fuera de rango lanza InvalidLocation`() {
        assertThrows(DomainError.InvalidLocation::class.java) {
            create(location = Location(0.0, -200.0, null))
        }
    }
}
