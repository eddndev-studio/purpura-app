package com.eddndev.purpura.ui.support

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.Reminder
import java.time.Instant

// Constructor de eventos de muestra para las pruebas de ViewModel.
internal fun sampleEvent(
    id: String = "e1",
    type: EventType = EventType.junta,
    status: EventStatus = EventStatus.pendiente,
    description: String = "Revision de avance",
    reminder: Reminder = Reminder.ten_minutes_before,
): Event = Event(
    id = id,
    userId = "u1",
    type = type,
    contact = Contact("Maria", null),
    location = Location(19.4, -99.1, "Campus"),
    description = description,
    startsAt = Instant.parse("2026-06-10T15:30:00Z"),
    status = status,
    reminder = reminder,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

// Documento de respaldo de muestra para las pruebas de Respaldo/Restaurar.
internal fun sampleExportDocument(
    events: List<Event> = listOf(sampleEvent()),
): ExportDocument = ExportDocument(
    schemaVersion = "1",
    exportedAt = Instant.EPOCH,
    userId = "u1",
    count = events.size,
    events = events,
)
