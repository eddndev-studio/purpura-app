package com.eddndev.purpura.ui.query

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.usecase.query.QueryEventsUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.ZoneId
import javax.inject.Inject

// Consultar (REQ-QUERY-001..006). A diferencia de Inicio (cache reactivo), aqui la consulta es
// una operacion puntual contra la API: el VM mantiene el estado imperativamente. `search` corre
// la pagina 1 y reemplaza; `loadMore` trae la siguiente y acumula. Errores -> @StringRes (un solo
// uso), por consistencia con HomeViewModel/AuthViewModel.
@HiltViewModel
class QueryViewModel @Inject constructor(
    private val queryEvents: QueryEventsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(QueryUiState.Initial)
    val uiState: StateFlow<QueryUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    // El init ya carga la pagina 1: la PRIMERA entrada a RESUMED no debe re-consultar (ver onResumed).
    private var initialLoadDone = false

    init {
        // Carga inicial sin filtros: muestra los eventos del usuario paginados por fecha.
        search(QueryFilters())
    }

    // Hot reload al volver al primer plano (integridad de datos). A diferencia de Inicio/Calendario
    // (Flow del cache de Room, reactivos), aqui la lista es un SNAPSHOT imperativo contra la API: al
    // volver del Detalle tras cambiar estatus, editar o borrar un evento, la pagina ya mostrada queda
    // obsoleta. Por eso re-pedimos la pagina 1 con los filtros vigentes en CADA regreso a RESUMED.
    // Esto dispara tambien en rotacion y en app background->foreground, no solo en Detalle->atras; es
    // DELIBERADO: re-consultar en cada reanudado es INVARIANTE al ciclo de vida y garantiza datos
    // correctos siempre (y respeta la pertenencia al filtro -- un evento que dejo de cumplir el
    // estatus filtrado desaparece -- cosa que un overlay del cache no podria). Un simple flag "vengo
    // del Detalle" parece mas fino pero reintroduce el bug: no sobrevive la recreacion por rotacion
    // (si rotas ESTANDO en el Detalle, el VM persiste obsoleto pero el flag se pierde -> lista stale
    // al volver). Compromiso aceptado en esta pantalla de consulta: esos reanudados reinician a la
    // pagina 1 (se pierde el scroll y las paginas acumuladas con loadMore). Saltamos la PRIMERA
    // entrada a RESUMED porque init ya cargo. search() conserva los resultados hasta tener los nuevos
    // (solo enciende la barra de progreso), de modo que el refresco no parpadea ni vacia la lista.
    fun onResumed() {
        if (!initialLoadDone) {
            initialLoadDone = true
            return
        }
        search(_uiState.value.filters)
    }

    fun search(filters: QueryFilters) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(filters = filters, isLoading = true, errorRes = null) }
            runCatching { queryEvents(filters.toQuery(page = 1)) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            events = page.items,
                            isLoading = false,
                            hasSearched = true,
                            loadedPages = page.pagination.page,
                            canLoadMore = page.pagination.page < page.pagination.totalPages,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isLoading = false, hasSearched = true, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    // Trae la siguiente pagina y la agrega al final. Ignora la llamada si ya hay carga en vuelo o
    // no quedan paginas; un fallo NO avanza el contador, asi que el usuario puede reintentar.
    fun loadMore() {
        val current = _uiState.value
        if (current.isLoading || current.isPaging || !current.canLoadMore) return
        val nextPage = current.loadedPages + 1
        searchJob = viewModelScope.launch {
            _uiState.update { it.copy(isPaging = true, errorRes = null) }
            runCatching { queryEvents(current.filters.toQuery(page = nextPage)) }
                .onSuccess { page ->
                    _uiState.update {
                        it.copy(
                            events = it.events + page.items,
                            isPaging = false,
                            loadedPages = page.pagination.page,
                            canLoadMore = page.pagination.page < page.pagination.totalPages,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isPaging = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }

    // tz = zona del dispositivo para que la API evalue las fronteras temporales como dias LOCALES
    // (consistente con Inicio y con lo que ve el usuario).
    private fun QueryFilters.toQuery(page: Int): EventQuery = EventQuery(
        mode = mode,
        date = date,
        from = from,
        to = to,
        year = year,
        month = month,
        type = type,
        status = status,
        page = page,
        pageSize = PAGE_SIZE,
        tz = ZoneId.systemDefault().id,
    )

    private companion object {
        const val PAGE_SIZE = 20
    }
}
