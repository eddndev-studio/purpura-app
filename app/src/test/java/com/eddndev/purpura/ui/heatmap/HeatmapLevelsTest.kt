package com.eddndev.purpura.ui.heatmap

import org.junit.Assert.assertEquals
import org.junit.Test

// Escala absoluta de intensidad: 0,1,2,3 directos y 4+ saturado en el nivel maximo.
class HeatmapLevelsTest {

    @Test
    fun `mapea el conteo a 5 niveles`() {
        assertEquals(0, HeatmapLevels.level(0))
        assertEquals(1, HeatmapLevels.level(1))
        assertEquals(2, HeatmapLevels.level(2))
        assertEquals(3, HeatmapLevels.level(3))
        assertEquals(4, HeatmapLevels.level(4))
    }

    @Test
    fun `satura en el nivel maximo a partir de cinco eventos`() {
        assertEquals(HeatmapLevels.MAX_LEVEL, HeatmapLevels.level(5))
        assertEquals(HeatmapLevels.MAX_LEVEL, HeatmapLevels.level(20))
    }
}
