package com.eddndev.purpura.ui.query

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import java.time.LocalDate

// Filtros activos de Consultar (REQ-QUERY-001..006). El modo decide que campos de fecha aplican:
// por_dia -> date; por_rango -> from/to; por_mes -> year/month; por_anio -> year. mode/type/status
// nulos significan "sin filtro".
data class QueryFilters(
    val mode: QueryMode? = null,
    val type: EventType? = null,
    val status: EventStatus? = null,
    val date: LocalDate? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val year: Int? = null,
    val month: Int? = null,
)

// Estado de Consultar. La lista se reemplaza al buscar (pagina 1) y se acumula al paginar.
// `isLoading` cubre la busqueda/refiltrado; `isPaging` la carga de la siguiente pagina. Un error
// no borra los resultados actuales: solo emite un aviso de un solo uso (consistente con Inicio).
data class QueryUiState(
    val events: List<Event>,
    val isLoading: Boolean,
    val isPaging: Boolean,
    val canLoadMore: Boolean,
    val loadedPages: Int,
    @StringRes val errorRes: Int?,
    val filters: QueryFilters,
    val hasSearched: Boolean,
) {
    // Vacio real solo tras una busqueda terminada sin resultados (no durante la carga).
    val isEmpty: Boolean get() = events.isEmpty() && !isLoading && hasSearched

    companion object {
        val Initial = QueryUiState(
            events = emptyList(),
            isLoading = false,
            isPaging = false,
            canLoadMore = false,
            loadedPages = 0,
            errorRes = null,
            filters = QueryFilters(),
            hasSearched = false,
        )
    }
}
