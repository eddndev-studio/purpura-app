package com.eddndev.purpura.ui.calendar

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventType
import java.time.LocalDate
import java.time.YearMonth

// Una celda de la rejilla del mes: vacia (relleno para alinear) o un dia con su resumen de eventos.
sealed interface CalendarCell {
    data object Empty : CalendarCell

    data class Day(
        val date: LocalDate,
        val typeDots: List<EventType>,   // tipos distintos presentes ese dia (puntos de color)
        val eventCount: Int,
        val isToday: Boolean,
        val isSelected: Boolean,
    ) : CalendarCell
}

// Estado del Calendario mensual (REQ-CAL-001..003). El cache (Room) es la fuente de los eventos;
// `isLoading` refleja el refresh contra la API; `errorRes` es un aviso de un solo uso. Siempre hay
// un dia seleccionado para que la lista inferior muestre algo (ver CalendarViewModel).
data class CalendarUiState(
    val yearMonth: YearMonth,
    val cells: List<CalendarCell>,
    val selectedDate: LocalDate?,
    val selectedDayEvents: List<Event>,
    val isLoading: Boolean,
    @StringRes val errorRes: Int?,
) {
    companion object {
        fun initial(yearMonth: YearMonth) = CalendarUiState(
            yearMonth = yearMonth,
            cells = emptyList(),
            selectedDate = null,
            selectedDayEvents = emptyList(),
            isLoading = true,
            errorRes = null,
        )
    }
}
