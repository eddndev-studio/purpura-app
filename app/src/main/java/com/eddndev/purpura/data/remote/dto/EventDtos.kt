package com.eddndev.purpura.data.remote.dto

// DTOs de evento (claves JSON camelCase, contrato §2). Forma PLANA para Contact y Location
// (contrato §2.2). Los instantes viajan como String RFC3339; la conversion a Instant ocurre
// en el mapper. Los opcionales (contactRef, locationLabel) son nullable con default.

data class EventDto(
    val id: String,
    val userId: String,
    val eventType: String,
    val contactName: String = "",
    val contactRef: String? = null,
    val locationLat: Double,
    val locationLng: Double,
    val locationLabel: String? = null,
    val description: String,
    val startsAt: String,
    val eventStatus: String,
    val reminderType: String,
    val createdAt: String,
    val updatedAt: String,
)

// POST /events (contrato §5.5). NO incluye userId ni eventStatus: el propietario sale del
// JWT y el estatus inicial siempre es pendiente.
data class CreateEventRequest(
    val eventType: String,
    val contactName: String,
    val contactRef: String? = null,
    val locationLat: Double,
    val locationLng: Double,
    val locationLabel: String? = null,
    val description: String,
    val startsAt: String,
    val reminderType: String,
)

// PATCH /events/{id} (contrato §5.8). Todos opcionales; solo los presentes se aplican.
data class UpdateEventRequest(
    val eventType: String? = null,
    val contactName: String? = null,
    val contactRef: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val locationLabel: String? = null,
    val description: String? = null,
    val startsAt: String? = null,
    val reminderType: String? = null,
)

// PATCH /events/{id}/status (contrato §5.9).
data class ChangeStatusRequest(
    val eventStatus: String,
)

// GET /events (contrato §5.7.4): envoltorio paginado.
data class PagedEventsResponse(
    val data: List<EventDto>,
    val pagination: PaginationDto,
)

data class PaginationDto(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val sort: String,
)
