package com.eddndev.purpura.ui.query

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.theme.Pill
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Consultar (REQ-QUERY-001..006) en Compose. Arriba un bloque de filtros no scrollable
 * (modo / tipo / estatus) con filas de [FilterChip] desplazables en horizontal; abajo la lista de
 * resultados ([LazyColumn] de [EventCard]) con paginacion, spinner inicial, spinner de pie y estado
 * vacio. La logica de datos vive en [QueryViewModel]; esta pantalla solo recibe estado y callbacks.
 *
 * Los date pickers NO viven aqui: al elegir un modo temporal (o tocar el boton de fecha) se invoca
 * [onPickDate] y el Fragment (que conserva MaterialDatePicker / MonthYearPicker) resuelve la fecha y
 * vuelve a llamar a la busqueda. Al cambiar tipo/estatus o volver a "Todos" se construye un
 * [QueryFilters] nuevo y se llama [onSearch]. Las fechas elegidas llegan dentro de [state]`.filters`.
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            QueryFilterBar(
                filters = state.filters,
                onSearch = onSearch,
                onPickDate = onPickDate,
            )
            QueryResults(
                state = state,
                onLoadMore = onLoadMore,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// --- Bloque de filtros (no scrollable verticalmente; cada fila desplaza en horizontal) ---

@Composable
private fun QueryFilterBar(
    filters: QueryFilters,
    onSearch: (QueryFilters) -> Unit,
    onPickDate: (QueryMode) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Periodo: al elegir un modo temporal se delega en el Fragment (onPickDate); "Todos" limpia
        // las fechas y dispara la busqueda de inmediato.
        FilterLabel(stringResource(R.string.query_period_label))
        ChipRow {
            FilterChipPill(
                selected = filters.mode == null,
                onClick = { onSearch(QueryFilters(type = filters.type, status = filters.status)) },
                label = stringResource(R.string.filter_all),
            )
            ModeChip(QueryMode.por_dia, R.string.query_mode_day, filters, onPickDate)
            ModeChip(QueryMode.por_rango, R.string.query_mode_range, filters, onPickDate)
            ModeChip(QueryMode.por_mes, R.string.query_mode_month, filters, onPickDate)
            ModeChip(QueryMode.por_anio, R.string.query_mode_year, filters, onPickDate)
        }

        // Boton de fecha: visible solo con un modo temporal activo. Su texto muestra la fecha / rango
        // / mes / anio elegido (o el placeholder del modo). Al tocarlo se reabre el selector.
        AnimatedVisibility(visible = filters.mode != null) {
            val mode = filters.mode
            OutlinedButton(
                onClick = { mode?.let(onPickDate) },
                shape = Pill,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(dateButtonLabel(filters))
            }
        }

        // Tipo: preserva el modo/fechas actuales y aplica de inmediato.
        FilterLabel(stringResource(R.string.query_type_label))
        ChipRow {
            FilterChipPill(
                selected = filters.type == null,
                onClick = { onSearch(filters.copy(type = null)) },
                label = stringResource(R.string.filter_all),
            )
            TypeChip(EventType.cita, filters, onSearch)
            TypeChip(EventType.junta, filters, onSearch)
            TypeChip(EventType.entrega_proyecto, filters, onSearch)
            TypeChip(EventType.examen, filters, onSearch)
            TypeChip(EventType.otros, filters, onSearch)
        }

        // Estatus: preserva el modo/fechas actuales y aplica de inmediato.
        FilterLabel(stringResource(R.string.query_status_label))
        ChipRow {
            FilterChipPill(
                selected = filters.status == null,
                onClick = { onSearch(filters.copy(status = null)) },
                label = stringResource(R.string.filter_all),
            )
            StatusChip(EventStatus.pendiente, filters, onSearch)
            StatusChip(EventStatus.realizado, filters, onSearch)
            StatusChip(EventStatus.aplazado, filters, onSearch)
        }
    }
}

@Composable
private fun ModeChip(
    mode: QueryMode,
    labelRes: Int,
    filters: QueryFilters,
    onPickDate: (QueryMode) -> Unit,
) {
    FilterChipPill(
        selected = filters.mode == mode,
        // Cambiar de modo no busca aun: necesita fecha. El Fragment abre el selector y al elegir
        // vuelve a llamar a la busqueda con el filtro completo.
        onClick = { onPickDate(mode) },
        label = stringResource(labelRes),
    )
}

@Composable
private fun TypeChip(
    type: EventType,
    filters: QueryFilters,
    onSearch: (QueryFilters) -> Unit,
) {
    FilterChipPill(
        selected = filters.type == type,
        onClick = { onSearch(filters.copy(type = type)) },
        label = stringResource(EventDisplay.typeLabel(type)),
    )
}

@Composable
private fun StatusChip(
    status: EventStatus,
    filters: QueryFilters,
    onSearch: (QueryFilters) -> Unit,
) {
    FilterChipPill(
        selected = filters.status == status,
        onClick = { onSearch(filters.copy(status = status)) },
        label = stringResource(EventDisplay.statusLabel(status)),
    )
}

@Composable
private fun FilterChipPill(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    // El borde por defecto de FilterChip se conserva (evitamos filterChipBorder, cuya firma cambia
    // entre versiones de Material3); solo forzamos la forma pill como en el resto de la app.
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = Pill,
    )
}

@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
private fun FilterLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// --- Resultados: lista paginada + estados de carga / vacio ---

@Composable
private fun QueryResults(
    state: QueryUiState,
    onLoadMore: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(targetState = state.isEmpty, label = "query_results") { isEmpty ->
        if (isEmpty) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = stringResource(R.string.query_empty_title),
                    body = stringResource(R.string.query_empty_body),
                )
            }
        } else {
            QueryList(
                state = state,
                onLoadMore = onLoadMore,
                onEventClick = onEventClick,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun QueryList(
    state: QueryUiState,
    onLoadMore: () -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Paginacion: cuando el ultimo visible se acerca al final (size - 3) pedimos la siguiente pagina.
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            last >= state.events.size - PAGING_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        ) {
            items(state.events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            // Spinner de pie mientras se trae la siguiente pagina.
            if (state.isPaging) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Spinner inicial: cubre la busqueda / refiltrado cuando aun no hay resultados.
        AnimatedVisibility(visible = state.isLoading && state.events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// --- Etiqueta del boton de fecha a partir de los filtros (las fechas elegidas viven en el estado) ---

@Composable
private fun dateButtonLabel(filters: QueryFilters): String = when (filters.mode) {
    QueryMode.por_dia -> filters.date?.let(DATE_LABEL::format)
        ?: stringResource(R.string.query_pick_day)
    QueryMode.por_rango -> {
        val from = filters.from
        val to = filters.to
        if (from != null && to != null) {
            stringResource(R.string.query_range_format, DATE_LABEL.format(from), DATE_LABEL.format(to))
        } else {
            stringResource(R.string.query_pick_range)
        }
    }
    QueryMode.por_mes -> {
        val year = filters.year
        val month = filters.month
        if (year != null && month != null) MonthYearPicker.label(year, month)
        else stringResource(R.string.query_pick_month)
    }
    QueryMode.por_anio -> filters.year?.toString() ?: stringResource(R.string.query_pick_year)
    null -> stringResource(R.string.query_pick_day)
}

private const val PAGING_THRESHOLD = 3

// Mismo formato "d MMM yyyy" (es-MX) que usaba el Fragment para el texto del boton de fecha.
private val DATE_LABEL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX"))
