package com.eddndev.purpura.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.colorsFor
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

// Locale fijo del calendario (es-MX): determina el primer dia de la semana (domingo) y, por ello, el
// orden del encabezado de dias y de la rejilla (debe coincidir con el del ViewModel/MonthGrid).
private val CALENDAR_LOCALE: Locale = Locale("es", "MX")

// Alto aproximado de una celda de dia (numero + fila de puntos + margenes). Sirve para dimensionar la
// rejilla, que no hace scroll propio (solo la lista del dia seleccionado scrollea), igual que el XML.
private val CELL_HEIGHT = 56.dp
private const val MAX_DOTS = 3

/**
 * Calendario mensual (REQ-CAL-001..003) en Compose. Cabecera de mes con flechas, encabezado de dias
 * de la semana, rejilla de 7 columnas con puntos por tipo (resaltando hoy y el dia seleccionado) y,
 * debajo, la lista de eventos del dia seleccionado. La rejilla no hace scroll propio: solo la lista
 * inferior scrollea (como el XML). La logica vive en [CalendarViewModel]; esta pantalla solo recibe
 * estado y callbacks (la navegacion al Detalle la resuelve el Fragment). El error es un aviso de un
 * solo uso (snackbar).
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onEventClick: (Event) -> Unit,
    onErrorShown: () -> Unit,
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            MonthHeader(
                label = EventDisplay.formatMonth(state.yearMonth),
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
            )

            // Barra de progreso del refresh contra la API (equivalente al loadingBar del XML). Solo se
            // muestra mientras isLoading; al ocultarse no ocupa espacio (no desplaza la rejilla).
            AnimatedVisibility(visible = state.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            WeekdayHeaderRow(labels = weekdayLabels)
            Spacer(Modifier.height(4.dp))

            MonthGridView(
                cells = state.cells,
                onSelectDate = onSelectDate,
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            Text(
                text = state.selectedDate?.let(EventDisplay::formatFullDate).orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            SelectedDaySection(
                events = state.selectedDayEvents,
                isLoading = state.isLoading,
                onEventClick = onEventClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// Cabecera del mes: flecha anterior, etiqueta centrada y flecha siguiente.
@Composable
private fun MonthHeader(
    label: String,
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
            text = label,
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

// Una celda de dia: numero + hasta MAX_DOTS puntos de color por tipo. Resalta hoy (borde de marca) y
// el dia seleccionado (fondo de marca) con transicion de color suave.
@Composable
private fun DayCell(
    cell: CalendarCell.Day,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        targetValue = if (cell.isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        label = "dayCellBackground",
    )
    val textColor by animateColorAsState(
        targetValue = when {
            cell.isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
            cell.isToday -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dayCellText",
    )
    val shape = RoundedCornerShape(12.dp)
    val borderColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .height(CELL_HEIGHT)
            .padding(2.dp)
            .clip(shape)
            .background(background, shape)
            .then(
                if (cell.isToday && !cell.isSelected) {
                    Modifier.border(1.5.dp, borderColor, shape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = cell.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
        Spacer(Modifier.height(2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            cell.typeDots.take(MAX_DOTS).forEach { type ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(colorsFor(type).strong, CircleShape),
                )
            }
        }
    }
}

// Lista de eventos del dia seleccionado. Si esta vacia (y no esta cargando) muestra el texto de dia
// sin eventos. AnimatedVisibility entre vacio y lista para una aparicion suave.
@Composable
private fun SelectedDaySection(
    events: List<Event>,
    isLoading: Boolean,
    onEventClick: (Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(visible = events.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.calendar_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        AnimatedVisibility(visible = events.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    EventCard(
                        event = event,
                        onClick = { onEventClick(event) },
                        modifier = Modifier
                            .animateItem()
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}
