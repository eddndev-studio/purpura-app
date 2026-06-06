package com.eddndev.purpura.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

// Rejilla mensual pura y deterministica (semana iniciando en domingo, como es-MX).
class MonthGridTest {

    private val sunday = DayOfWeek.SUNDAY

    @Test
    fun `febrero 2026 empieza en domingo sin relleno inicial`() {
        val cells = MonthGrid.cells(YearMonth.of(2026, 2), sunday)

        assertEquals(28, cells.size) // 28 dias, lead 0, ya multiplo de 7
        assertEquals(LocalDate.of(2026, 2, 1), cells.first())
        assertEquals(LocalDate.of(2026, 2, 28), cells.last())
    }

    @Test
    fun `junio 2026 lleva una celda de relleno inicial`() {
        val cells = MonthGrid.cells(YearMonth.of(2026, 6), sunday)

        assertNull(cells[0]) // lead 1: el 1 de junio cae en lunes
        assertEquals(LocalDate.of(2026, 6, 1), cells[1])
        assertEquals(0, cells.size % 7) // rejilla rectangular
        assertEquals(LocalDate.of(2026, 6, 30), cells.filterNotNull().last())
    }

    @Test
    fun `las etiquetas de dias empiezan en domingo para es-MX`() {
        val labels = MonthGrid.weekdayLabels(sunday, Locale("es", "MX"))

        assertEquals(7, labels.size)
        assertEquals("Dom", labels.first())
    }
}
