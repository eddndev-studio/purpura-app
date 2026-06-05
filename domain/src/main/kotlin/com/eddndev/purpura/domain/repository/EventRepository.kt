package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Puerto del dominio: espejo de /api/v1/events/* (06-app-architecture §4.2). Devuelve
// modelos de dominio (no DTO) y lanza DomainError (no excepciones HTTP). La capa data lo
// implementa orquestando Retrofit + cache de Room (politica offline §9).
interface EventRepository {

    // Lectura reactiva desde el cache; refresca contra la API por debajo.
    fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>>   // REQ-HOME-001
    fun observeMonth(year: Int, month: Int): Flow<List<Event>>                 // REQ-CAL-001 / heatmap

    suspend fun refreshRange(from: LocalDate, to: LocalDate)                   // sincroniza cache
    suspend fun query(query: EventQuery): Page<Event>                         // REQ-QUERY-001..006
    suspend fun getById(id: String): Event                                    // REQ-QUERY-007, REQ-CAL-002

    suspend fun create(draft: NewEventDraft): Event                           // POST /events (status=pendiente)
    suspend fun update(id: String, patch: EventPatch): Event                  // PATCH /events/{id}
    suspend fun changeStatus(id: String, status: EventStatus): Event          // PATCH /events/{id}/status
    suspend fun delete(id: String)                                            // DELETE /events/{id}

    suspend fun export(query: EventQuery? = null): ExportDocument             // GET /events/export
    suspend fun import(request: ImportRequest): ImportResult                  // POST /events/import
}
