package com.eddndev.purpura.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.compose.DayChip
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.EventCardSkeleton
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.compose.colorsFor
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalDate

// Filas de la rejilla de esqueleto: 6 cubre cualquier mes (max 6 semanas visibles).
private const val SKELETON_ROWS = 6

// Rejilla del mes + encabezado del dia seleccionado + lista de eventos. Es el contenido que el
// AnimatedContent de la pantalla desliza por mes, asi que recibe el estado del mes mostrado.
@Composable
internal fun GridAndDayList(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onAddEvent: (LocalDate) -> Unit,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        MonthGridView(cells = state.cells, onSelectDate = onSelectDate)

        // Separa la rejilla de la lista del dia con aire de seccion (en vez de un divisor duro).
        Spacer(Modifier.height(Spacing.section))

        val selected = state.selectedDate
        if (selected != null) {
            // Encabezado del dia en titleMedium (mas prominente que un SectionHeader) y, debajo, el
            // contador del dia en el slot trailing de un SectionHeader (labelLarge/primary).
            Text(
                text = EventDisplay.formatFullDate(selected),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            SectionHeader(
                text = "",
                trailing = {
                    Text(
                        text = state.selectedDayEvents.size.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        }

        SelectedDaySection(
            events = state.selectedDayEvents,
            isLoading = state.isLoading,
            onAddEvent = { selected?.let(onAddEvent) },
            onEventClick = onEventClick,
            modifier = Modifier.weight(1f),
        )
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
    LazyVerticalGrid(
        columns = GridCells.Fixed(MonthGrid.DAYS_PER_WEEK),
        modifier = Modifier
            .fillMaxWidth()
            .height(CELL_HEIGHT * rows),
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
                    onSelect = { onSelectDate(cell.date) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

// Una celda de dia: el chip de marca (relleno del seleccionado, anillo de hoy) arriba y los puntos
// por tipo DEBAJO (no dentro del chip). La celda entera es el area de toque; clearAndSetSemantics
// fusiona dia + tipos + hoy/seleccion + conteo en una sola descripcion accesible y re-expone el
// click (clearAndSet descarta el del chip), dejando los puntos como decorado no anunciado.
@Composable
private fun DayCell(
    cell: CalendarCell.Day,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Reusa el plural del heatmap (conteo + fecha) para no duplicar texto; antepone "Hoy" si aplica.
    val countDesc = pluralStringResource(
        R.plurals.heatmap_day_count,
        cell.eventCount,
        cell.eventCount,
        EventDisplay.formatFullDate(cell.date),
    )
    val todayLabel = stringResource(R.string.action_today)
    val cellDesc = if (cell.isToday) "$todayLabel, $countDesc" else countDesc

    Column(
        modifier = modifier
            .height(CELL_HEIGHT)
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .clearAndSetSemantics {
                contentDescription = cellDesc
                selected = cell.isSelected
                onClick { onSelect(); true }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        DayChip(
            day = cell.date.dayOfMonth.toString(),
            selected = cell.isSelected,
            isToday = cell.isToday,
            onClick = onSelect,
        )
        Spacer(Modifier.height(Spacing.xxs))
        TypeDots(cell.typeDots)
    }
}

// Fila de puntos por tipo. Si hay mas tipos que MAX_DOTS, se condensa en 2 puntos + "+N" para no
// saturar la celda (N = tipos restantes). Decorativo: la semantica la pone la celda.
@Composable
private fun TypeDots(types: List<com.eddndev.purpura.domain.model.EventType>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xxs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (types.size > MAX_DOTS) {
            types.take(2).forEach { Dot(colorsFor(it).strong) }
            Text(
                text = "+${types.size - 2}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            types.forEach { Dot(colorsFor(it).strong) }
        }
    }
}

@Composable
private fun Dot(color: androidx.compose.ui.graphics.Color) {
    Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
}

// Lista de eventos del dia seleccionado. Vacia (y sin cargar) -> EmptyState con accion directa que
// pre-rellena el dia seleccionado. Crossfade entre vacio y lista para una aparicion suave.
@Composable
private fun SelectedDaySection(
    events: List<Event>,
    isLoading: Boolean,
    onAddEvent: () -> Unit,
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
                    action = {
                        Button(onClick = onAddEvent, shape = Pill) {
                            Text(stringResource(R.string.fab_add_event))
                        }
                    },
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

// Esqueleto en frio: rejilla de discos (huecos de celda) con la misma huella que la real + un par de
// EventCardSkeleton para la lista del dia. Sustituye el "spinner sobre vacio".
@Composable
internal fun CalendarSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(SKELETON_ROWS) {
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(MonthGrid.DAYS_PER_WEEK) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(CELL_HEIGHT),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(Spacing.section))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
            repeat(3) { EventCardSkeleton() }
        }
    }
}
