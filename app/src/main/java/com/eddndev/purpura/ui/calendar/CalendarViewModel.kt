package com.eddndev.purpura.ui.calendar

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

// Calendario mensual (REQ-CAL-001..003). Observa el cache (Room) del mes visible y lo combina con
// la fecha seleccionada y el estado del refresh contra la API. Mismo patron reactivo que Inicio:
// stateIn sobre un Flow, el cache es la fuente de verdad y un refresh fallido NO borra la rejilla.
// El mes se pliega DENTRO del flujo de eventos para que rejilla y puntos cambien de forma atomica
// al navegar (sin un parpadeo con eventos del mes anterior).
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getMonthEvents: GetMonthEventsUseCase,
    private val refreshMonthEvents: RefreshMonthEventsUseCase,
) : ViewModel() {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val firstDayOfWeek: DayOfWeek = WeekFields.of(LOCALE).firstDayOfWeek
    // TODO(#8): `today` se congela al construir el VM (igual que Inicio). Aceptable salvo que la
    // app quede viva cruzando la medianoche; mitigacion futura compartida con HomeViewModel.
    private val today: LocalDate = LocalDate.now(zone)

    private val yearMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow<LocalDate?>(null)
    private val refreshing = MutableStateFlow(false)
    private val errorRes = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    private val monthWithEvents = yearMonth.flatMapLatest { ym ->
        getMonthEvents(ym.year, ym.monthValue).map { events -> ym to events }
    }

    val uiState: StateFlow<CalendarUiState> = combine(
        monthWithEvents,
        selectedDate,
        refreshing,
        errorRes,
    ) { monthEvents, selected, isRefreshing, error ->
        buildState(monthEvents.first, monthEvents.second, selected, isRefreshing, error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = CalendarUiState.initial(YearMonth.from(today)),
    )

    init {
        refresh(yearMonth.value)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun nextMonth() = goToMonth(yearMonth.value.plusMonths(1))

    fun previousMonth() = goToMonth(yearMonth.value.minusMonths(1))

    fun errorShown() {
        errorRes.value = null
    }

    // Al cambiar de mes se reinicia la seleccion (null -> se recalcula la seleccion por defecto del
    // nuevo mes) y se sincroniza ese mes contra la API.
    private fun goToMonth(target: YearMonth) {
        yearMonth.value = target
        selectedDate.value = null
        refresh(target)
    }

    // Sincroniza el mes objetivo. Un nuevo mes supera (cancela) un refresh en vuelo para que gane
    // el ultimo solicitado. toErrorMessageRes RE-LANZA CancellationException: cancelar el refresh
    // anterior al navegar de mes NO debe pintar un aviso de error. Un fallo real conserva el cache.
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
        selected: LocalDate?,
        isRefreshing: Boolean,
        error: Int?,
    ): CalendarUiState {
        val byDay = events.groupBy { it.startsAt.atZone(zone).toLocalDate() }
        val effectiveSelected = selected ?: defaultSelection(ym, byDay)
        val cells = MonthGrid.cells(ym, firstDayOfWeek).map { date ->
            if (date == null) {
                CalendarCell.Empty
            } else {
                val dayEvents = byDay[date].orEmpty()
                CalendarCell.Day(
                    date = date,
                    typeDots = dayEvents.map { it.type }.distinct(),
                    eventCount = dayEvents.size,
                    isToday = date == today,
                    isSelected = date == effectiveSelected,
                )
            }
        }
        val dayEvents = byDay[effectiveSelected].orEmpty().sortedBy { it.startsAt }
        return CalendarUiState(ym, cells, effectiveSelected, dayEvents, isRefreshing, error)
    }

    // Seleccion por defecto al abrir/cambiar de mes: hoy si cae en el mes; si no, el primer dia con
    // eventos; si ninguno, el dia 1 (siempre hay algo seleccionado para la lista inferior).
    private fun defaultSelection(ym: YearMonth, byDay: Map<LocalDate, List<Event>>): LocalDate {
        if (YearMonth.from(today) == ym) return today
        val firstWithEvents = byDay.keys.filter { YearMonth.from(it) == ym }.minOrNull()
        return firstWithEvents ?: ym.atDay(1)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val LOCALE: Locale = Locale("es", "MX")
    }
}
