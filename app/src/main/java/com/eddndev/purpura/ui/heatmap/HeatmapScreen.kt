package com.eddndev.purpura.ui.heatmap

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.compose.MonthNavHeader
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.compose.WeekdayHeaderRow
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
 * La rejilla y la cabecera de dias viven en una Surface de contenedor (tarjeta) para separar el mapa
 * del resto; debajo van la leyenda y el resumen del mes, agrupados con ritmo de seccion. Tocar un dia
 * muestra su conteo en un Snackbar local. La logica vive en [HeatmapViewModel].
 *
 * Nota: el contenido entero comparte un unico scroll vertical, por eso la rejilla se construye NO-lazy
 * (chunked(7) -> filas) en vez de un LazyVerticalGrid (ver [HeatmapGrid] en HeatmapScreenParts).
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

    // Etiquetas de dias en el orden del locale (Dom..Sab en es-MX), igual que MonthGrid.cells.
    val weekdayLabels = remember {
        val firstDayOfWeek = WeekFields.of(LOCALE).firstDayOfWeek
        MonthGrid.weekdayLabels(firstDayOfWeek, LOCALE)
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
        // Carga FRIA: no hay cache aun (rejilla vacia) y se esta refrescando -> esqueleto en vez de
        // barra. El refresh con cache (rejilla ya poblada) solo muestra la barra delgada reservada.
        val coldLoad = state.cells.isEmpty() && state.isLoading
        val cacheRefresh = state.cells.isNotEmpty() && state.isLoading

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(start = Spacing.screenH, end = Spacing.screenH, top = Spacing.xs, bottom = Spacing.lg),
        ) {
            MonthNavHeader(
                monthLabel = EventDisplay.formatMonth(state.yearMonth),
                onPrev = onPrevMonth,
                onNext = onNextMonth,
            )

            // Ranura reservada para la barra de refresh: ocupa su alto siempre (no desplaza la rejilla
            // al aparecer/desaparecer); la barra solo se pinta en refresh con cache.
            Box(modifier = Modifier.fillMaxWidth().height(Spacing.xs)) {
                // La ranura ya reserva el alto, asi que un if directo no desplaza la rejilla (sin
                // AnimatedVisibility: su overload de ColumnScope no resuelve dentro del BoxScope).
                if (cacheRefresh) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            // Tarjeta contenedora: encabezado de dias + rejilla (o esqueleto) agrupados como un bloque.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    WeekdayHeaderRow(labels = weekdayLabels)
                    Crossfade(targetState = coldLoad, label = "heatmapColdLoad") { cold ->
                        if (cold) {
                            HeatmapSkeletonGrid()
                        } else {
                            HeatmapGrid(cells = state.cells, onDayClick = onDayClick)
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.section))
            HeatmapLegend()

            Spacer(Modifier.height(Spacing.section))
            MonthSummary(total = state.totalEvents)
        }
    }
}
