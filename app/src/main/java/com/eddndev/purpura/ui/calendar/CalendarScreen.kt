package com.eddndev.purpura.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.compose.colorsFor
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

// Locale fijo del calendario (es-MX): determina el primer dia de la semana (domingo) y, por ello, el
// orden del encabezado de dias y de la rejilla (debe coincidir con el del ViewModel/MonthGrid).
private val CALENDAR_LOCALE: Locale = Locale("es", "MX")

// Alto comodo de una celda de dia (>=48dp segun spec): numero + fila de puntos con aire. Dimensiona la
// rejilla, que no hace scroll propio (solo la lista del dia seleccionado scrollea), igual que el XML.
private val CELL_HEIGHT = 56.dp
private const val MAX_DOTS = 3

// Modos de vista del Calendario para el toggle segmentado del app bar. "Mes" es la rejilla de esta
// pantalla; "Calor" navega al Mapa de calor (lo resuelve el Fragment via onShowHeatmap).
enum class CalendarView { MONTH, HEAT }

/**
 * Calendario mensual (REQ-CAL-001..003) en Compose. Top-level (sin back): vive bajo [PurpuraScreen]
 * con un SegmentedToggle "Mes / Calor" en el app bar (al elegir Calor se navega al Mapa de calor).
 * Cabecera de mes con flechas grandes y "Junio 2026" centrado, encabezado de dias, rejilla de 7
 * columnas con celdas comodas y puntos por tipo (resaltando hoy y el dia seleccionado) y, debajo, la
 * lista de eventos del dia seleccionado. La rejilla no hace scroll propio: solo la lista inferior
 * scrollea. La logica vive en [CalendarViewModel]; esta pantalla solo recibe estado y callbacks (la
 * navegacion al Detalle/Calor la resuelve el Fragment). El error es un aviso de un solo uso (snackbar).
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (Event) -> Unit,
    onErrorShown: () -> Unit,
    onShowHeatmap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    // Etiquetas de dias en el orden del locale (Dom..Sab en es-MX), igual que MonthGrid.cells.
    val weekdayLabels = remember {
        val firstDayOfWeek = WeekFields.of(CALENDAR_LOCALE).firstDayOfWeek
        MonthGrid.weekdayLabels(firstDayOfWeek, CALENDAR_LOCALE)
    }

    PurpuraScreen(
        title = stringResource(R.string.title_calendar),
        modifier = modifier,
        large = false,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            // Toggle Mes/Calor: "Mes" siempre seleccionado aqui; elegir "Calor" navega al heatmap.
            SegmentedToggle(
                options = CalendarView.entries,
                selected = CalendarView.MONTH,
                onSelect = { view -> if (view == CalendarView.HEAT) onShowHeatmap() },
                labelOf = { view ->
                    when (view) {
                        CalendarView.MONTH -> stringResource(R.string.calendar_view_month)
                        CalendarView.HEAT -> stringResource(R.string.calendar_view_heat)
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
                .padding(horizontal = Spacing.screenH),
        ) {
            MonthHeader(
                label = EventDisplay.formatMonth(state.yearMonth),
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
            )

            // Barra de progreso del refresh contra la API. Solo se muestra mientras isLoading; al
            // ocultarse no ocupa espacio (no desplaza la rejilla).
            AnimatedVisibility(visible = state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            WeekdayHeaderRow(labels = weekdayLabels)
            Spacer(Modifier.height(Spacing.xs))

            MonthGridView(
                cells = state.cells,
                onSelectDate = onSelectDate,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Spacing.md),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SectionHeader(
                text = state.selectedDate?.let(EventDisplay::formatFullDate).orEmpty(),
            )

            SelectedDaySection(
                events = state.selectedDayEvents,
                isLoading = state.isLoading,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Cabecera del mes: flecha anterior, etiqueta centrada y flecha siguiente. Flechas grandes (48dp de
// toque) para una navegacion comoda; el mes/anio en titleLarge centrado.
@Composable
private fun MonthHeader(
    label: String,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevMonth, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = stringResource(R.string.calendar_prev_month),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
        // Crossfade del titulo para que el cambio de mes se sienta como una transicion, no un salto.
        Crossfade(targetState = label, label = "monthLabel", modifier = Modifier.weight(1f)) { current ->
            Text(
                text = current,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
        IconButton(onClick = onNextMonth, modifier = Modifier.size(48.dp)) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = stringResource(R.string.calendar_next_month),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

// Fila de 7 etiquetas de dias de la semana, distribuidas en partes iguales.
@Composable
private fun WeekdayHeaderRow(labels: List<String>) {
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

// Rejilla de 7 columnas con las celdas del mes. No hace scroll propio: se dimensiona a su contenido
// (filas * alto de celda) para que solo la lista del dia seleccionado scrollee, como en el XML.
@Composable
private fun MonthGridView(
    cells: List<CalendarCell>,
    onSelectDate: (LocalDate) -> Unit,
) {
    val rows = if (cells.isEmpty()) 0 else (cells.size + MonthGrid.DAYS_PER_WEEK - 1) / MonthGrid.DAYS_PER_WEEK
    val gridDesc = stringResource(R.string.calendar_grid_desc)
    LazyVerticalGrid(
        columns = GridCells.Fixed(MonthGrid.DAYS_PER_WEEK),
        modifier = Modifier
            .fillMaxWidth()
            .height(CELL_HEIGHT * rows)
            .semantics { contentDescription = gridDesc },
        userScrollEnabled = false,
    ) {
        itemsIndexed(
            items = cells,
            // Clave estable y unica por posicion: las celdas Empty son un singleton (data object) y
            // no pueden distinguirse por valor, asi que se usa el indice de la rejilla.
            key = { index, cell ->
                when (cell) {
                    is CalendarCell.Day -> "day-${cell.date}"
                    is CalendarCell.Empty -> "empty-$index"
                }
            },
        ) { _, cell ->
            when (cell) {
                is CalendarCell.Empty -> Box(modifier = Modifier.height(CELL_HEIGHT))
                is CalendarCell.Day -> DayCell(
                    cell = cell,
                    onClick = { onSelectDate(cell.date) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

// Una celda de dia: numero + hasta MAX_DOTS puntos de color por tipo. Resalta hoy (borde de marca,
// animado) y el dia seleccionado (fondo de marca). El fondo, el texto y el grosor del borde animan
// para que la seleccion se sienta fluida.
@Composable
private fun DayCell(
    cell: CalendarCell.Day,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (cell.isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        label = "dayCellBackground",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            cell.isSelected -> MaterialTheme.colorScheme.onPrimary
            cell.isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dayCellText",
    )
    // Hoy se marca con borde de marca solo cuando no esta seleccionado (el fondo ya lo distingue).
    val borderWidth by animateDpAsState(
        targetValue = if (cell.isToday && !cell.isSelected) 1.5.dp else 0.dp,
        label = "dayCellBorder",
    )
    val shape = CircleShape

    Column(
        modifier = modifier
            .height(CELL_HEIGHT)
            .padding(Spacing.xs)
            .clip(shape)
            .background(background, shape)
            .border(borderWidth, MaterialTheme.colorScheme.primary, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
        Spacer(Modifier.height(Spacing.xxs))
        // Puntos de tipo: sobre dia seleccionado usan onPrimary para contrastar con el fondo de marca.
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            cell.typeDots.take(MAX_DOTS).forEach { type ->
                val dotColor = if (cell.isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    colorsFor(type).strong
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(dotColor, CircleShape),
                )
            }
        }
    }
}

// Lista de eventos del dia seleccionado. Si esta vacia (y no esta cargando) muestra un EmptyState del
// kit. Crossfade entre vacio y lista para una aparicion suave.
@Composable
private fun SelectedDaySection(
    events: List<Event>,
    isLoading: Boolean,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = events.isEmpty(),
        label = "selectedDayContent",
        modifier = modifier.fillMaxSize(),
    ) { isEmpty ->
        if (isEmpty) {
            // No mostrar el vacio mientras carga: evita parpadeo entre dias durante el refresh.
            AnimatedVisibility(visible = !isLoading) {
                EmptyState(
                    icon = Icons.Outlined.EventBusy,
                    title = stringResource(R.string.calendar_day_empty_title),
                    body = stringResource(R.string.calendar_day_empty),
                    modifier = Modifier.heightIn(min = 160.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.item),
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}
