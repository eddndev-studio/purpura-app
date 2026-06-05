package com.eddndev.purpura.data.remote.mapper

import com.eddndev.purpura.data.mapper.EnumCodec
import com.eddndev.purpura.data.remote.dto.CreateEventRequest
import com.eddndev.purpura.data.remote.dto.EventDto
import com.eddndev.purpura.data.remote.dto.ExportDocumentDto
import com.eddndev.purpura.data.remote.dto.ImportErrorDto
import com.eddndev.purpura.data.remote.dto.ImportRequestDto
import com.eddndev.purpura.data.remote.dto.ImportResultDto
import com.eddndev.purpura.data.remote.dto.PagedEventsResponse
import com.eddndev.purpura.data.remote.dto.UpdateEventRequest
import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportError
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.query.Pagination
import java.time.Instant
import javax.inject.Inject

// Traduce DTO Retrofit <-> dominio (06-app-architecture §6). El dominio nunca importa
// EventDto. Reconstruye Event con el constructor (no la factory): los eventos del servidor
// ya son validos y su estatus no debe forzarse a pendiente.
class EventRemoteMapper @Inject constructor() {

    fun toDomain(dto: EventDto): Event = Event(
        id = dto.id,
        userId = dto.userId,
        type = EnumCodec.eventType(dto.eventType),
        contact = Contact(dto.contactName, EnumCodec.emptyToNull(dto.contactRef)),
        location = Location(dto.locationLat, dto.locationLng, EnumCodec.emptyToNull(dto.locationLabel)),
        description = dto.description,
        startsAt = Instant.parse(dto.startsAt),
        status = EnumCodec.eventStatus(dto.eventStatus),
        reminder = EnumCodec.reminder(dto.reminderType),
        createdAt = Instant.parse(dto.createdAt),
        updatedAt = Instant.parse(dto.updatedAt),
    )

    fun toDto(event: Event): EventDto = EventDto(
        id = event.id,
        userId = event.userId,
        eventType = event.type.name,
        contactName = event.contact.name,
        contactRef = event.contact.ref,
        locationLat = event.location.lat,
        locationLng = event.location.lng,
        locationLabel = event.location.label,
        description = event.description,
        startsAt = event.startsAt.toString(),
        eventStatus = event.status.name,
        reminderType = event.reminder.name,
        createdAt = event.createdAt.toString(),
        updatedAt = event.updatedAt.toString(),
    )

    fun toCreateRequest(draft: NewEventDraft): CreateEventRequest = CreateEventRequest(
        eventType = draft.type.name,
        contactName = draft.contact.name,
        contactRef = draft.contact.ref,
        locationLat = draft.location.lat,
        locationLng = draft.location.lng,
        locationLabel = draft.location.label,
        description = draft.description,
        startsAt = draft.startsAt.toString(),
        reminderType = draft.reminder.name,
    )

    // PATCH parcial. Un campo nulo del patch se omite; un contacto/ubicacion presente con su
    // opcional en null se envia como "" para limpiarlo (contrato §5.8).
    fun toUpdateRequest(patch: EventPatch): UpdateEventRequest = UpdateEventRequest(
        eventType = patch.type?.name,
        contactName = patch.contact?.name,
        contactRef = patch.contact?.let { it.ref ?: "" },
        locationLat = patch.location?.lat,
        locationLng = patch.location?.lng,
        locationLabel = patch.location?.let { it.label ?: "" },
        description = patch.description,
        startsAt = patch.startsAt?.toString(),
        reminderType = patch.reminder?.name,
    )

    fun toPage(response: PagedEventsResponse): Page<Event> = Page(
        items = response.data.map(::toDomain),
        pagination = with(response.pagination) {
            Pagination(page, pageSize, totalItems, totalPages, sort)
        },
    )

    fun toExportDocument(dto: ExportDocumentDto): ExportDocument = ExportDocument(
        schemaVersion = dto.schemaVersion,
        exportedAt = Instant.parse(dto.exportedAt),
        userId = dto.userId,
        count = dto.count,
        events = dto.events.map(::toDomain),
    )

    fun toImportRequestDto(request: ImportRequest): ImportRequestDto = ImportRequestDto(
        mode = request.mode.name,
        events = request.events.map(::toDto),
    )

    fun toImportResult(dto: ImportResultDto): ImportResult = ImportResult(
        imported = dto.imported,
        updated = dto.updated,
        skipped = dto.skipped,
        failed = dto.failed,
        errors = dto.errors.map { it.toDomain() },
    )

    private fun ImportErrorDto.toDomain() = ImportError(index, code, detail)
}
