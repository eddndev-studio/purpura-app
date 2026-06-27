package com.eddndev.purpura.ui.query

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.PurpuraBottomSheet
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SkeletonList
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Consultar (REQ-QUERY-001..006) en Compose, rediseñada sobre el kit. Pantalla top-level (sin back).
 * Los filtros viven en una hoja "Filtros" ([PurpuraBottomSheet]) que se abre desde la accion del app
 * bar, de modo que la lista de resultados ocupa todo el viewport. Cuando hay filtros activos se fija
 * arriba de la lista un resumen de chips ([ActiveFiltersRow]) que reabre la hoja al tocarlo.
 *
 * Estados: una barra lineal fija bajo el app bar cubre cualquier busqueda/refiltrado en curso (haya o
 * no resultados, sin saltar el layout); en frio se muestran esqueletos en vez de pantalla en blanco;
 * el vacio ofrece "Limpiar filtros" si habia filtros. La logica de datos vive en [QueryViewModel].
 *
 * Los date pickers NO viven aqui: al elegir un modo temporal (o tocar el boton de fecha) se invoca
 * [onPickDate] y el Fragment (que conserva MaterialDatePicker / MonthYearPicker) resuelve la fecha y
 * vuelve a llamar a la busqueda. La firma se conserva para el Fragment.
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
    var showFilters by remember { mutableStateOf(false) }

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
        actions = {
            IconButton(onClick = { showFilters = true }) {
                Icon(
                    imageVector = Icons.Outlined.FilterList,
                    contentDescription = stringResource(R.string.query_filters),
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Barra de progreso fija bajo el app bar mientras se busca/refiltra, haya o no resultados.
            // A diferencia del spinner centrado, no reserva ni salta espacio del contenido.
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
            QueryContent(
                state = state,
                onSearch = onSearch,
                onLoadMore = onLoadMore,
                onEventClick = onEventClick,
                onOpenFilters = { showFilters = true },
                modifier = Modifier.weight(1f),
            )
        }
    }

    // Hoja "Filtros": aloja el bloque completo de filtros para liberar el viewport de la lista. El
    // boton inferior solo cierra la hoja: los filtros se aplican al tocar cada chip (onSearch).
    if (showFilters) {
        PurpuraBottomSheet(
            onDismiss = { showFilters = false },
            title = stringResource(R.string.query_filters),
        ) {
            QueryFilterBar(
                filters = state.filters,
                onSearch = onSearch,
                onPickDate = onPickDate,
            )
            Button(
                onClick = { showFilters = false },
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.query_filters_apply))
            }
        }
    }
}

@Composable
private fun QueryContent(
    state: QueryUiState,
    onSearch: (QueryFilters) -> Unit,
    onLoadMore: () -> Unit,
    onEventClick: (Event) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Paginacion: cuando el ultimo visible se acerca al final (size - umbral) pedimos otra pagina.
    // Guardamos por lista no vacia + canLoadMore para no disparar onLoadMore en reposo.
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

    // Carga en frio: esqueletos en vez de pantalla en blanco (mismo patron que Inicio). La barra
    // lineal del app bar ya comunica el progreso por encima.
    if (state.events.isEmpty() && state.isLoading) {
        SkeletonList(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm),
        )
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        // El hueco superior unico lo posee contentPadding top=sm; los items no agregan padding vertical.
        contentPadding = PaddingValues(
            start = Spacing.screenH,
            end = Spacing.screenH,
            top = Spacing.sm,
            bottom = Spacing.section,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.item),
    ) {
        if (state.events.isEmpty()) {
            // Tras buscar sin coincidencias: estado vacio centrado en el viewport restante.
            item(key = "results_state") {
                QueryEmptyArea(
                    state = state,
                    onClearFilters = { onSearch(QueryFilters()) },
                    modifier = Modifier.fillParentMaxSize(),
                )
            }
        } else {
            // Resumen de filtros activos fijado arriba de los resultados; reabre la hoja al tocarlo.
            if (state.filters.hasActiveFilters) {
                item(key = "summary") {
                    ActiveFiltersRow(filters = state.filters, onClick = onOpenFilters)
                }
            }
            item(key = "results_header") {
                ResultsHeader(count = state.events.size)
            }
            items(state.events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    modifier = Modifier.animateItem(),
                )
            }
            // Cierre de la lista: ya no quedan paginas que traer.
            if (!state.canLoadMore) {
                item(key = "no_more") { NoMoreResults() }
            }
        }

        // Spinner de pie mientras se trae la siguiente pagina.
        if (state.isPaging) {
            item(key = "paging_spinner") { PagingSpinner() }
        }
    }
}

// Area sin resultados. Tras una busqueda sin coincidencias muestra el vacio (con "Limpiar filtros"
// si habia filtros activos). El prompt inicial es residual: el VM busca al iniciar. Durante la carga
// en frio no pinta nada aqui (los esqueletos de QueryContent ya cubren ese caso).
@Composable
private fun QueryEmptyArea(
    state: QueryUiState,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = state.isEmpty, label = "query_empty_area", modifier = modifier) { isEmpty ->
        when {
            isEmpty -> EmptyState(
                icon = Icons.Outlined.Search,
                title = stringResource(R.string.query_empty_title),
                body = stringResource(R.string.query_empty_body),
                action = if (state.filters.hasActiveFilters) {
                    {
                        Button(onClick = onClearFilters, shape = Pill) {
                            Text(stringResource(R.string.query_clear_filters))
                        }
                    }
                } else {
                    null
                },
            )
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
