package com.eddndev.purpura.ui.common

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

// Logica pura (sin Android) de la rejilla de un mes, compartida por Calendario y Mapa de calor.
// El primer dia de la semana se deriva del locale (es-MX empieza en domingo); se recibe como
// parametro para poder fijarlo en las pruebas y mantener la rejilla deterministica.
object MonthGrid {

    const val DAYS_PER_WEEK = 7

    // Celdas en orden de lectura: nulls iniciales para alinear el dia 1 bajo su columna y nulls
    // finales para completar la ultima semana (rejilla rectangular de 7 columnas).
    fun cells(yearMonth: YearMonth, firstDayOfWeek: DayOfWeek): List<LocalDate?> {
        val first = yearMonth.atDay(1)
        val lead = Math.floorMod(first.dayOfWeek.value - firstDayOfWeek.value, DAYS_PER_WEEK)
        val length = yearMonth.lengthOfMonth()
        val cells = ArrayList<LocalDate?>(lead + length)
        repeat(lead) { cells.add(null) }
        for (day in 1..length) cells.add(yearMonth.atDay(day))
        while (cells.size % DAYS_PER_WEEK != 0) cells.add(null)
        return cells
    }

    // Etiquetas cortas de los 7 dias empezando en firstDayOfWeek (Dom Lun Mar... en es-MX).
    fun weekdayLabels(firstDayOfWeek: DayOfWeek, locale: Locale): List<String> =
        (0 until DAYS_PER_WEEK).map { offset ->
            firstDayOfWeek.plus(offset.toLong())
                .getDisplayName(TextStyle.SHORT, locale)
                .replaceFirstChar { it.uppercase(locale) }
        }
}
