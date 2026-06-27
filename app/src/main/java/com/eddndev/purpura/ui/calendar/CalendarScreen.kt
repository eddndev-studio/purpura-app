package com.eddndev.purpura.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.compose.MonthNavHeader
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.compose.WeekdayHeaderRow
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

// Locale fijo del calendario (es-MX): determina el primer dia de la semana (domingo) y, por ello, el
// orden del encabezado de dias y de la rejilla (debe coincidir con el del ViewModel/MonthGrid).
private val CALENDAR_LOCALE: Locale = Locale("es", "MX")

// Alto de una celda de dia: chip de 40dp arriba + fila de puntos por tipo debajo, con aire. Dimensiona
// la rejilla, que no hace scroll propio (solo la lista del dia seleccionado scrollea), igual que el XML.
internal val CELL_HEIGHT = 60.dp

// A partir de cuantos tipos distintos se condensa la fila de puntos en "2 puntos + N" (ver DayCell).
internal const val MAX_DOTS = 3

// Modos de vista del Calendario para el toggle segmentado del app bar. "Mes" es la rejilla de esta
// pantalla; "Calor" navega al Mapa de calor (lo resuelve el Fragment via onShowHeatmap).
enum class CalendarView { MONTH, HEAT }

// Fases de la pantalla: en frio (sin celdas y cargando) se muestran esqueletos; en caliente, el
// contenido real. El refresh con cache ya pintado NO vuelve a Loading (usa la barra de progreso).
private enum class CalendarPhase { Loading, Content }

/**
 * Calendario mensual (REQ-CAL-001..003) en Compose. Top-level (sin back): vive bajo [PurpuraScreen]
 * con un SegmentedToggle "Mes / Calor" en el app bar (al elegir Calor se navega al Mapa de calor).
 * Cabecera de mes con flechas y acceso "Hoy", encabezado de dias y, debajo, la rejilla del mes con
 * puntos por tipo y la lista de eventos del dia seleccionado. La rejilla y la lista se envuelven en un
 * AnimatedContent por mes para que el cambio de mes deslice de forma atomica; en frio, un Crossfade
 * cruza de esqueletos a contenido. La logica vive en [CalendarViewModel].
 */
@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onSelectDate: (LocalDate) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onAddEvent: (LocalDate) -> Unit,
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

    // En frio aun no hay celdas (el cache no ha emitido): se muestran esqueletos en vez de un hueco.
    val phase = if (state.cells.isEmpty() && state.isLoading) CalendarPhase.Loading else CalendarPhase.Content

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
            MonthNavHeader(
                monthLabel = EventDisplay.formatMonth(state.yearMonth),
                onPrev = onPrevMonth,
                onNext = onNextMonth,
                onToday = onToday,
            )

            RefreshBar(isLoading = state.isLoading)

            Spacer(Modifier.height(Spacing.sm))
            WeekdayHeaderRow(labels = weekdayLabels)
            Spacer(Modifier.height(Spacing.xs))

            // En frio, esqueletos; en caliente, contenido. El cambio de mes (deslizamiento) vive
            // DENTRO de la fase Content para que esqueletos y deslizamiento no compitan.
            Crossfade(
                targetState = phase,
                animationSpec = tween(200),
                label = "calendarPhase",
                modifier = Modifier.fillMaxSize(),
            ) { current ->
                when (current) {
                    CalendarPhase.Loading -> CalendarSkeleton()
                    CalendarPhase.Content -> AnimatedContent(
                        targetState = state,
                        // Solo anima al cambiar de mes; seleccionar dia o alternar carga NO desliza.
                        contentKey = { it.yearMonth },
                        transitionSpec = {
                            // Direccion segun avance/retroceso de mes para que el gesto se sienta natural.
                            val forward = targetState.yearMonth.isAfter(initialState.yearMonth)
                            val dir = if (forward) 1 else -1
                            val spec = tween<IntOffset>(250, easing = FastOutSlowInEasing)
                            (slideInHorizontally(spec) { full -> dir * full } + fadeIn(tween(250)))
                                .togetherWith(
                                    slideOutHorizontally(spec) { full -> -dir * full } + fadeOut(tween(250)),
                                )
                        },
                        label = "monthContent",
                        modifier = Modifier.fillMaxSize(),
                    ) { shown ->
                        GridAndDayList(
                            state = shown,
                            onSelectDate = onSelectDate,
                            onAddEvent = onAddEvent,
                            onEventClick = onEventClick,
                        )
                    }
                }
            }
        }
    }
}

// Barra de progreso del refresh contra la API. Reserva SIEMPRE su alto (la del indicador) aunque no
// este cargando, para que alternar isLoading no desplace la rejilla (un salto delataria la demo).
@Composable
private fun RefreshBar(isLoading: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
