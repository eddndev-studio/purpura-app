package com.eddndev.purpura.ui.heatmap

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.YearMonth

// Mapea el conteo de eventos de un dia a uno de 5 niveles de intensidad (heatmap_0..heatmap_4).
// Escala ABSOLUTA (0, 1, 2, 3, 4+): estable entre meses y facil de leer. Una escala relativa al
// maximo del mes resaltaria mas el contraste; se eligio la absoluta por claridad de "densidad".
object HeatmapLevels {
    const val MAX_LEVEL = 4
    fun level(count: Int): Int = count.coerceIn(0, MAX_LEVEL)
}

// Una celda del mapa de calor: vacia (relleno) o un dia con su conteo y nivel de intensidad.
sealed interface HeatmapCell {
    data object Empty : HeatmapCell

    data class Day(
        val date: LocalDate,
        val count: Int,
        val level: Int,
        val isToday: Boolean,
    ) : HeatmapCell
}

// Estado del Mapa de calor (densidad de eventos por dia del mes). El cache (Room) es la fuente;
// `isLoading` refleja el refresh; `errorRes` es un aviso de un solo uso.
data class HeatmapUiState(
    val yearMonth: YearMonth,
    val cells: List<HeatmapCell>,
    val totalEvents: Int,
    val isLoading: Boolean,
    @StringRes val errorRes: Int?,
) {
    companion object {
        fun initial(yearMonth: YearMonth) = HeatmapUiState(
            yearMonth = yearMonth,
            cells = emptyList(),
            totalEvents = 0,
            isLoading = true,
            errorRes = null,
        )
    }
}
