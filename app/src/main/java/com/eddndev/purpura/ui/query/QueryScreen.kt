package com.eddndev.purpura.ui.query

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.LoadingState
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Consultar (REQ-QUERY-001..006) en Compose, rediseñada sobre el kit. Pantalla top-level (sin back),
 * TopAppBar normal. Una sola [LazyColumn] hospeda primero el bloque de filtros (Periodo / Tipo /
 * Estatus con [SectionHeader] y chips que reflujan en FlowRow) y luego los resultados como [EventCard]
 * paginados, asi el app bar reacciona al scroll y todo respira con [Spacing].
 *
 * Estados: spinner inicial cubre la busqueda cuando aun no hay resultados; un spinner de pie aparece
 * al traer la siguiente pagina; el contenido (prompt inicial / vacio / lista) se intercambia con
 * [Crossfade] para evitar saltos. La logica de datos vive en [QueryViewModel]: esta pantalla solo
 * recibe estado y callbacks y conserva la firma que usa el Fragment.
 *
 * Los date pickers NO viven aqui: al elegir un modo temporal (o tocar el boton de fecha) se invoca
 * [onPickDate] y el Fragment (que conserva MaterialDatePicker / MonthYearPicker) resuelve la fecha y
 * vuelve a llamar a la busqueda. Al cambiar tipo/estatus o volver a "Todos" se construye un
 * [QueryFilters] nuevo y se llama [onSearch]. Las fechas elegidas llegan dentro de [state].filters.
 */
@Composable
fun QueryScreen(
    state: QueryUiState,
    onSearch: (QueryFilters) -> Unit,
    onLoadMore: () -> Unit,
    onPickDate: (QueryMode) -> Unit,
    onEventClick: (Event) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Aviso de un solo uso (consistente con Inicio): un error no borra los resultados actuales.
    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    PurpuraScreen(
        title = stringResource(R.string.title_query),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            QueryContent(
                state = state,
                onSearch = onSearch,
                onLoadMore = onLoadMore,
                onPickDate = onPickDate,
                onEventClick = onEventClick,
            )

            // Spinner inicial del kit: cubre la busqueda / refiltrado mientras aun no hay resultados.
            if (state.isLoading && state.events.isEmpty()) {
                LoadingState()
            }
        }
    }
}

@Composable
private fun QueryContent(
    state: QueryUiState,
    onSearch: (QueryFilters) -> Unit,
    onLoadMore: () -> Unit,
    onPickDate: (QueryMode) -> Unit,
    onEventClick: (Event) -> Unit,
) {
    val listState = rememberLazyListState()

    // Paginacion: cuando el ultimo visible se acerca al final (size - umbral) pedimos otra pagina.
    // Guardamos por lista no vacia + canLoadMore: como el bloque de filtros es el item 0, la lista
    // nunca esta vacia en layout, asi que sin esta guarda dispararia onLoadMore en reposo / sin
    // resultados (el VM ya lo ignora, pero evitamos la llamada redundante por frame).
    val shouldLoadMore by remember {
        derivedStateOf {
            if (state.events.isEmpty() || !state.canLoadMore) return@derivedStateOf false
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            last >= state.events.size - PAGING_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = Spacing.sm,
            bottom = Spacing.fabClearance,
        ),
    ) {
        // Bloque de filtros como cabecera de la lista para que colapse junto al app bar.
        item(key = "filters") {
            QueryFilterBar(
                filters = state.filters,
                onSearch = onSearch,
                onPickDate = onPickDate,
                modifier = Modifier.padding(
                    horizontal = Spacing.screenH,
                    vertical = Spacing.sm,
                ),
            )
        }

        // Resultados o estado vacio/prompt. El estado se intercambia con Crossfade y se centra en lo
        // que resta del viewport via fillParentMaxSize (mismo patron que Inicio).
        if (state.events.isEmpty()) {
            item(key = "results_state") {
                QueryEmptyArea(
                    state = state,
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        } else {
            items(state.events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = Spacing.screenH, vertical = Spacing.xs),
                )
            }
        }

        // Spinner de pie mientras se trae la siguiente pagina.
        if (state.isPaging) {
            item(key = "paging_spinner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// Area sin resultados: antes de buscar invita a hacerlo (prompt); tras buscar sin coincidencias
// muestra el estado vacio. Se intercambian con Crossfade y se centran en el viewport restante.
// Durante la carga inicial no pinta nada: el spinner de QueryScreen ya cubre ese caso.
@Composable
private fun QueryEmptyArea(
    state: QueryUiState,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = state.isEmpty, label = "query_empty_area", modifier = modifier) { isEmpty ->
        when {
            isEmpty -> EmptyState(
                icon = Icons.Outlined.Search,
                title = stringResource(R.string.query_empty_title),
                body = stringResource(R.string.query_empty_body),
            )
            // Prompt inicial solo cuando no hay carga en curso (evita parpadeo bajo el spinner).
            // Reusa placeholder_query como cuerpo (mismo patron que Inicio con placeholder_home).
            !state.isLoading && !state.hasSearched -> EmptyState(
                icon = Icons.Outlined.CalendarMonth,
                title = stringResource(R.string.query_start_title),
                body = stringResource(R.string.placeholder_query),
            )
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

private const val PAGING_THRESHOLD = 3
