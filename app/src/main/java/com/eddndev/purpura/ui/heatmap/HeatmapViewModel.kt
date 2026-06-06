package com.eddndev.purpura.ui.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.usecase.calendar.GetMonthEventsUseCase
import com.eddndev.purpura.domain.usecase.calendar.RefreshMonthEventsUseCase
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

// Mapa de calor mensual: densidad de eventos por dia (REQ-HEATMAP). Mismo patron reactivo y de
// refresh que el Calendario (comparten GetMonthEventsUseCase y MonthGrid), pero sin seleccion ni
// lista: cada celda solo lleva su conteo y nivel de intensidad. El cache es la fuente de verdad.
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val getMonthEvents: GetMonthEventsUseCase,
    private val refreshMonthEvents: RefreshMonthEventsUseCase,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val firstDayOfWeek: DayOfWeek = WeekFields.of(LOCALE).firstDayOfWeek
    private val today: LocalDate = LocalDate.now(zone)

    private val yearMonth = MutableStateFlow(YearMonth.from(today))
    private val refreshing = MutableStateFlow(false)
    private val errorRes = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    private val monthWithEvents = yearMonth.flatMapLatest { ym ->
        getMonthEvents(ym.year, ym.monthValue).map { events -> ym to events }
    }

    val uiState: StateFlow<HeatmapUiState> = combine(
        monthWithEvents,
        refreshing,
        errorRes,
    ) { monthEvents, isRefreshing, error ->
        buildState(monthEvents.first, monthEvents.second, isRefreshing, error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HeatmapUiState.initial(YearMonth.from(today)),
    )

    init {
        refresh(yearMonth.value)
    }

    fun nextMonth() = goToMonth(yearMonth.value.plusMonths(1))

    fun previousMonth() = goToMonth(yearMonth.value.minusMonths(1))

    fun errorShown() {
        errorRes.value = null
    }

    private fun goToMonth(target: YearMonth) {
        yearMonth.value = target
        refresh(target)
    }

    // toErrorMessageRes RE-LANZA CancellationException: cancelar el refresh anterior al navegar de
    // mes no debe pintar un aviso de error. Un fallo real conserva el cache y emite el aviso.
    private fun refresh(target: YearMonth) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            errorRes.value = null
            runCatching { refreshMonthEvents(target.year, target.monthValue) }
                .onFailure { errorRes.value = it.toErrorMessageRes() }
            refreshing.value = false
        }
    }

    private fun buildState(
        ym: YearMonth,
        events: List<Event>,
        isRefreshing: Boolean,
        error: Int?,
    ): HeatmapUiState {
        val countByDay = events.groupingBy { it.startsAt.atZone(zone).toLocalDate() }.eachCount()
        val cells = MonthGrid.cells(ym, firstDayOfWeek).map { date ->
            if (date == null) {
                HeatmapCell.Empty
            } else {
                val count = countByDay[date] ?: 0
                HeatmapCell.Day(date, count, HeatmapLevels.level(count), date == today)
            }
        }
        return HeatmapUiState(ym, cells, events.size, isRefreshing, error)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val LOCALE: Locale = Locale("es", "MX")
    }
}
