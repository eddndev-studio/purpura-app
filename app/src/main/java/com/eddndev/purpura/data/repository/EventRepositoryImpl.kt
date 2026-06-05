package com.eddndev.purpura.data.repository

import com.eddndev.purpura.data.local.dao.EventDao
import com.eddndev.purpura.data.local.mapper.EventEntityMapper
import com.eddndev.purpura.data.remote.api.EventApi
import com.eddndev.purpura.data.remote.dto.ChangeStatusRequest
import com.eddndev.purpura.data.remote.interceptor.ProblemErrorAdapter
import com.eddndev.purpura.data.remote.mapper.EventRemoteMapper
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.query.Page
import com.eddndev.purpura.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.time.LocalDate
import java.time.ZoneOffset
import javax.inject.Inject

// Orquesta remoto (Retrofit) + cache (Room) aplicando la politica offline (06-app §9):
// lecturas reactivas desde Room; escrituras requieren red y solo tocan el cache tras 2xx;
// la API Go es la unica fuente de verdad.
class EventRepositoryImpl @Inject constructor(
    private val eventApi: EventApi,
    private val eventDao: EventDao,
    private val remoteMapper: EventRemoteMapper,
    private val entityMapper: EventEntityMapper,
    private val errorAdapter: ProblemErrorAdapter,
    @IoDispatcher private val io: CoroutineDispatcher,
) : EventRepository {

    override fun observeUpcoming(today: LocalDate, daysAhead: Int): Flow<List<Event>> =
        eventDao.observeBetween(
            from = startOfDayMs(today),
            to = endOfDayMs(today.plusDays(daysAhead.toLong())),
        ).map { rows -> rows.map(entityMapper::toDomain) }

    override fun observeMonth(year: Int, month: Int): Flow<List<Event>> {
        val first = LocalDate.of(year, month, 1)
        return eventDao.observeBetween(
            from = startOfDayMs(first),
            to = endOfDayMs(first.plusMonths(1).minusDays(1)),
        ).map { rows -> rows.map(entityMapper::toDomain) }
    }

    override suspend fun refreshRange(from: LocalDate, to: LocalDate) {
        withContext(io) {
            errorAdapter.call {
                val response = eventApi.query(
                    buildParams(EventQuery(mode = QueryMode.por_rango, from = from, to = to, pageSize = MAX_PAGE)),
                )
                eventDao.upsertAll(response.data.map { entityMapper.toEntity(remoteMapper.toDomain(it)) })
            }
        }
    }

    override suspend fun query(query: EventQuery): Page<Event> = withContext(io) {
        errorAdapter.call {
            val page = remoteMapper.toPage(eventApi.query(buildParams(query)))
            eventDao.upsertAll(page.items.map(entityMapper::toEntity))
            page
        }
    }

    override suspend fun getById(id: String): Event = withContext(io) {
        errorAdapter.call {
            val event = remoteMapper.toDomain(eventApi.getById(id))
            eventDao.upsertAll(listOf(entityMapper.toEntity(event)))
            event
        }
    }

    override suspend fun create(draft: NewEventDraft): Event = withContext(io) {
        errorAdapter.call {
            val event = remoteMapper.toDomain(eventApi.create(remoteMapper.toCreateRequest(draft)))
            eventDao.upsertAll(listOf(entityMapper.toEntity(event)))
            event
        }
    }

    override suspend fun update(id: String, patch: EventPatch): Event = withContext(io) {
        errorAdapter.call {
            val event = remoteMapper.toDomain(eventApi.update(id, remoteMapper.toUpdateRequest(patch)))
            eventDao.upsertAll(listOf(entityMapper.toEntity(event)))
            event
        }
    }

    override suspend fun changeStatus(id: String, status: EventStatus): Event = withContext(io) {
        errorAdapter.call {
            val event = remoteMapper.toDomain(eventApi.changeStatus(id, ChangeStatusRequest(status.name)))
            eventDao.upsertAll(listOf(entityMapper.toEntity(event)))
            event
        }
    }

    override suspend fun delete(id: String) {
        withContext(io) {
            errorAdapter.call {
                val response = eventApi.delete(id)
                if (!response.isSuccessful) throw HttpException(response)
                eventDao.deleteById(id)
            }
        }
    }

    override suspend fun export(query: EventQuery?): ExportDocument = withContext(io) {
        errorAdapter.call {
            remoteMapper.toExportDocument(eventApi.export(query?.let(::buildParams) ?: emptyMap()))
        }
    }

    override suspend fun import(request: ImportRequest): ImportResult = withContext(io) {
        errorAdapter.call {
            remoteMapper.toImportResult(eventApi.import(remoteMapper.toImportRequestDto(request)))
        }
    }

    // Construye el query map de /events (contrato §5.7.1); omite los parametros nulos.
    private fun buildParams(query: EventQuery): Map<String, String> = buildMap {
        query.mode?.let { put("mode", it.name) }
        query.date?.let { put("date", it.toString()) }
        query.from?.let { put("from", it.toString()) }
        query.to?.let { put("to", it.toString()) }
        query.year?.let { put("year", it.toString()) }
        query.month?.let { put("month", it.toString()) }
        query.type?.let { put("type", it.name) }
        query.status?.let { put("status", it.name) }
        put("page", query.page.toString())
        put("pageSize", query.pageSize.toString())
        put("sort", query.sort)
        query.tz?.let { put("tz", it) }
    }

    private fun startOfDayMs(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private fun endOfDayMs(date: LocalDate): Long =
        date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() - 1

    private companion object {
        const val MAX_PAGE = 100
    }
}
