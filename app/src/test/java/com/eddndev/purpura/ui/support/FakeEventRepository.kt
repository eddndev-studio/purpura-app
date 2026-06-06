package com.eddndev.purpura.ui.support

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.query.Pagination
import com.eddndev.purpura.domain.repository.EventRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

// Fake configurable del puerto de eventos para las pruebas de ViewModel de Consultar y Detalle.
// Captura los argumentos recibidos y permite inyectar resultados o errores por operacion. Las
// operaciones no usadas por estos slices fallan ruidosamente si se invocan por error.
internal class FakeEventRepository : EventRepository {

    // query()
    val queries = mutableListOf<EventQuery>()
    var queryError: Throwable? = null
    val pages = ArrayDeque<Page<Event>>()
    var defaultPage: Page<Event> = emptyPage()

    // Si se asigna, query() se suspende esperando este gate (para probar busquedas en vuelo,
    // cancelacion/last-wins). La consulta ya quedo registrada en `queries` antes de suspenderse.
    var queryGate: CompletableDeferred<Unit>? = null

    // getById()
    val getByIdCalls = mutableListOf<String>()
    var getByIdError: Throwable? = null
    var event: Event? = null

    // changeStatus()
    val statusChanges = mutableListOf<Pair<String, EventStatus>>()
    var changeStatusError: Throwable? = null
    var changeStatusResult: Event? = null

    // delete()
    val deletedIds = mutableListOf<String>()
    var deleteError: Throwable? = null

    // update()
    val patches = mutableListOf<Pair<String, EventPatch>>()
    var updateError: Throwable? = null
    var updateResult: Event? = null

    // create()
    val createdDrafts = mutableListOf<NewEventDraft>()
    var createError: Throwable? = null
    var createResult: Event? = null

    // Si se asigna, create() se suspende esperando este gate (para probar el guard de envio en
    // curso). El draft ya quedo registrado en `createdDrafts` antes de suspenderse.
    var createGate: CompletableDeferred<Unit>? = null

    override suspend fun query(query: EventQuery): Page<Event> {
        queries += query
        queryGate?.await()
        queryError?.let { throw it }
        return pages.removeFirstOrNull() ?: defaultPage
    }

    override suspend fun getById(id: String): Event {
        getByIdCalls += id
        getByIdError?.let { throw it }
        return event ?: error("event no configurado en el fake")
    }

    override suspend fun changeStatus(id: String, status: EventStatus): Event {
        statusChanges += id to status
        changeStatusError?.let { throw it }
        return changeStatusResult ?: error("changeStatusResult no configurado en el fake")
    }

    override suspend fun delete(id: String) {
        deletedIds += id
        deleteError?.let { throw it }
    }

    override suspend fun update(id: String, patch: EventPatch): Event {
        patches += id to patch
        updateError?.let { throw it }
        return updateResult ?: error("updateResult no configurado en el fake")
    }

    override fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>> = notUsed()
    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> = notUsed()
    override suspend fun refreshRange(from: LocalDate, to: LocalDate) = notUsed()
    override suspend fun create(draft: NewEventDraft): Event {
        createdDrafts += draft
        createGate?.await()
        createError?.let { throw it }
        return createResult ?: error("createResult no configurado en el fake")
    }
    override suspend fun export(query: EventQuery?): ExportDocument = notUsed()
    override suspend fun import(request: ImportRequest): ImportResult = notUsed()

    private fun notUsed(): Nothing = error("no usado en estas pruebas")

    companion object {
        fun emptyPage(): Page<Event> =
            Page(emptyList(), Pagination(page = 1, pageSize = 20, totalItems = 0, totalPages = 0, sort = "startsAt:asc"))

        fun pageOf(items: List<Event>, page: Int, totalPages: Int): Page<Event> =
            Page(
                items = items,
                pagination = Pagination(
                    page = page,
                    pageSize = 20,
                    totalItems = totalPages * 20,
                    totalPages = totalPages,
                    sort = "startsAt:asc",
                ),
            )
    }
}
