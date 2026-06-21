package com.eddndev.purpura.ui.heatmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.theme.PurpuraTheme
import com.eddndev.purpura.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.time.temporal.WeekFields
import java.util.Locale

private val LOCALE: Locale = Locale("es", "MX")

// Modos de vista del mes: rejilla normal (Calendario) o mapa de calor. Sirve solo para el toggle del
// app bar; este destino siempre esta en CALOR, y elegir MES navega de vuelta al Calendario.
private enum class HeatmapViewMode { MONTH, HEAT }

/**
 * Mapa de calor mensual (densidad de eventos por dia) en Compose. Es un MODO DE VISTA del Calendario:
 * el app bar comparte el toggle "Mes / Calor" (Calor activo); elegir "Mes" llama [onShowCalendar].
 * Pinta una rejilla de 7 columnas con cada dia tenido por su nivel de intensidad
 * ([PurpuraTheme.colors.heatmap]); tocar un dia muestra su conteo en un Snackbar local. La cabecera
 * permite navegar de mes y un aviso de error es de un solo uso. La logica vive en [HeatmapViewModel].
 *
 * Nota: el contenido entero comparte un unico scroll vertical, por eso la rejilla se construye NO-lazy
 * (chunked(7) -> filas) en vez de un LazyVerticalGrid.
 */
@Composable
fun HeatmapScreen(
    state: HeatmapUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onShowCalendar: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            onErrorShown()
        }
    }

    // Tocar un dia: muestra su conteo de eventos sin salir de la pantalla.
    // pluralStringResource es @Composable; en un lambda hay que resolverlo via resources.
    val onDayClick: (HeatmapCell.Day) -> Unit = { cell ->
        val message = context.resources.getQuantityString(
            R.plurals.heatmap_day_count,
            cell.count,
            cell.count,
            EventDisplay.formatFullDate(cell.date),
        )
        scope.launch {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }

    PurpuraScreen(
        title = stringResource(R.string.title_heatmap),
        modifier = modifier,
        large = false,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            // Mismo toggle que el Calendario: aqui CALOR esta fijo activo; elegir MES vuelve al mes.
            SegmentedToggle(
                options = HeatmapViewMode.entries,
                selected = HeatmapViewMode.HEAT,
                onSelect = { mode -> if (mode == HeatmapViewMode.MONTH) onShowCalendar() },
                labelOf = { mode ->
                    // Reusa los strings del toggle del Calendario: es un unico control compartido entre
                    // ambas pantallas, asi las etiquetas Mes/Calor se leen identicas y hay un solo par.
                    when (mode) {
                        HeatmapViewMode.MONTH -> stringResource(R.string.calendar_view_month)
                        HeatmapViewMode.HEAT -> stringResource(R.string.calendar_view_heat)
                    }
                },
                modifier = Modifier.padding(end = Spacing.sm),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm),
        ) {
            MonthHeader(
                monthLabel = EventDisplay.formatMonth(state.yearMonth),
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
            )

            // Barra de carga delgada (el refresh conserva el cache, no se vacia la rejilla).
            AnimatedVisibility(visible = state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.xs),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            WeekdayHeader()
            Spacer(Modifier.height(Spacing.xs))
            HeatmapGrid(cells = state.cells, onDayClick = onDayClick)

            Spacer(Modifier.height(Spacing.section))
            HeatmapLegend()

            Spacer(Modifier.height(Spacing.lg))
            MonthTotal(total = state.totalEvents)
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

// Cabecera de mes: flecha anterior, etiqueta centrada (destacada) y flecha siguiente.
@Composable
private fun MonthHeader(
    monthLabel: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.calendar_prev_month),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = monthLabel,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onNextMonth) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// Encabezado de los 7 dias de la semana segun el locale (es-MX empieza en domingo). Reusa la logica
// pura de MonthGrid para que el orden coincida con el de las celdas.
@Composable
private fun WeekdayHeader() {
    val firstDayOfWeek = remember { WeekFields.of(LOCALE).firstDayOfWeek }
    val labels = remember { MonthGrid.weekdayLabels(firstDayOfWeek, LOCALE) }
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// Rejilla NO-lazy de 7 columnas: comparte el scroll del contenido (no anida scrolls). Cada fila es
// una semana; las celdas vacias rellenan para alinear el dia 1 y completar la ultima semana. El
// respiro entre celdas (spacedBy) hace que cada mosaico se lea como un cuadro independiente.
@Composable
private fun HeatmapGrid(
    cells: List<HeatmapCell>,
    onDayClick: (HeatmapCell.Day) -> Unit,
) {
    val gridDesc = stringResource(R.string.heatmap_grid_desc)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = gridDesc },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        cells.chunked(MonthGrid.DAYS_PER_WEEK).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                week.forEach { cell ->
                    HeatmapCellTile(
                        cell = cell,
                        onDayClick = onDayClick,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Una celda del mapa: dia tenido por intensidad o relleno vacio. Mosaico cuadrado con esquinas suaves
// (forma de tarjeta del design system, NO pill). El fondo se anima entre niveles 0..4 al cambiar de
// mes o al refrescar, para que la transicion de densidad se perciba.
@Composable
private fun HeatmapCellTile(
    cell: HeatmapCell,
    onDayClick: (HeatmapCell.Day) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Spacing.md)
    when (cell) {
        is HeatmapCell.Empty -> {
            // Relleno: ocupa la columna para mantener la rejilla rectangular, sin pintar nada.
            Box(modifier = modifier.aspectRatio(1f))
        }

        is HeatmapCell.Day -> {
            val heatmap = PurpuraTheme.colors.heatmap
            val targetColor = heatmap[cell.level.coerceIn(0, heatmap.lastIndex)]
            val background by animateColorAsState(targetColor, label = "heatmapCellColor")
            // Texto: onSurface salvo en el nivel mas intenso (4), que usa el token de etiqueta intensa.
            val textColor = if (cell.level >= HeatmapLevels.MAX_LEVEL) {
                PurpuraTheme.colors.heatmapLabelIntense
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val borderColor = if (cell.isToday) MaterialTheme.colorScheme.primary else Color.Transparent

            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(shape)
                    .background(background, shape)
                    .border(width = 2.dp, color = borderColor, shape = shape)
                    .clickable { onDayClick(cell) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cell.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// Leyenda horizontal: "menos" [5 muestras de nivel 0..4] "mas". Misma forma suave que las celdas para
// que la escala se lea como las del mapa.
@Composable
private fun HeatmapLegend() {
    val legendDesc = stringResource(R.string.heatmap_legend_desc)
    val heatmap = PurpuraTheme.colors.heatmap
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = legendDesc },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.heatmap_legend_less),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        heatmap.forEach { color ->
            Box(
                modifier = Modifier
                    .padding(horizontal = Spacing.xxs)
                    .size(width = 22.dp, height = 16.dp)
                    .clip(RoundedCornerShape(Spacing.xs))
                    .background(color),
            )
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.heatmap_legend_more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Total del mes centrado, con el numero destacado (titleLarge/primary) sobre la etiqueta secundaria,
// como pide la jerarquia tipografica del spec para conteos.
@Composable
private fun MonthTotal(total: Int) {
    Text(
        text = pluralStringResource(R.plurals.heatmap_month_total, total, total),
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
}
