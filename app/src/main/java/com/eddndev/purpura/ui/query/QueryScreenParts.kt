package com.eddndev.purpura.ui.query

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Bloque de filtros de Consultar: secciones Periodo / Tipo / Estatus con [SectionHeader] y chips que
 * reflujan en [FlowRow] (en vez de scroll horizontal, para verlos todos). El boton de fecha aparece
 * integrado bajo Periodo solo con un modo temporal activo. La logica de filtros/paginacion no cambia:
 * se construye un [QueryFilters] y se llama a onSearch, o se delega la fecha al Fragment via onPickDate.
 */
// FlowRowScope (receiver de las lambdas de chips) es experimental: el opt-in cubre el sitio de
// construccion ademas de FilterSection donde se consume.
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QueryFilterBar(
    filters: QueryFilters,
    onSearch: (QueryFilters) -> Unit,
    onPickDate: (QueryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Periodo: al elegir un modo temporal se delega en el Fragment (onPickDate); "Todos" limpia
        // las fechas y dispara la busqueda de inmediato.
        FilterSection(stringResource(R.string.query_period_label)) {
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
        AnimatedVisibility(
            visible = filters.mode != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            val mode = filters.mode
            OutlinedButton(
                onClick = { mode?.let(onPickDate) },
                shape = Pill,
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(dateButtonLabel(filters))
            }
        }

        // Tipo: preserva el modo/fechas actuales y aplica de inmediato.
        FilterSection(stringResource(R.string.query_type_label)) {
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
        FilterSection(stringResource(R.string.query_status_label)) {
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

// Encabezado de seccion + chips que reflujan. Centraliza el espaciado para las tres secciones.
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    chips: @Composable FlowRowScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        SectionHeader(text = title)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), content = chips)
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

// Etiqueta del boton de fecha a partir de los filtros (las fechas elegidas viven en el estado).
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

// Mismo formato "d MMM yyyy" (es-MX) que usaba el Fragment para el texto del boton de fecha.
private val DATE_LABEL: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX"))
