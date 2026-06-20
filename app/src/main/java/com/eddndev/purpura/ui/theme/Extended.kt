package com.eddndev.purpura.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Par fuerte/contenedor de un color semantico: `strong` es texto/icono, `container` es el fondo del
 * chip. Asi se renderiza un chip de estatus o tipo (texto fuerte sobre su propio container), con el
 * contraste AA verificado en la spec.
 */
@Immutable
data class ColorPair(val strong: Color, val container: Color)

/**
 * Colores semanticos de Purpura que NO son roles M3 (estatus, tipo de evento, heatmap). Se proveen
 * por [LocalPurpuraColors] y cambian con el tema claro/oscuro igual que el [androidx.compose.material3.ColorScheme].
 * Las pantallas obtienen estos tokens con `MaterialTheme` extendido via `PurpuraTheme.colors`.
 */
@Immutable
data class PurpuraExtendedColors(
    val statusPendiente: ColorPair,
    val statusRealizado: ColorPair,
    val statusAplazado: ColorPair,
    val eventCita: ColorPair,
    val eventJunta: ColorPair,
    val eventEntrega: ColorPair,
    val eventExamen: ColorPair,
    val eventOtros: ColorPair,
    val heatmap: List<Color>,
    val heatmapLabelIntense: Color,
    val onBrandSubtle: Color,
)

internal val LightExtendedColors = PurpuraExtendedColors(
    statusPendiente = ColorPair(StatusPendienteLight, StatusPendienteContainerLight),
    statusRealizado = ColorPair(StatusRealizadoLight, StatusRealizadoContainerLight),
    statusAplazado = ColorPair(StatusAplazadoLight, StatusAplazadoContainerLight),
    eventCita = ColorPair(EventCitaLight, EventCitaContainerLight),
    eventJunta = ColorPair(EventJuntaLight, EventJuntaContainerLight),
    eventEntrega = ColorPair(EventEntregaLight, EventEntregaContainerLight),
    eventExamen = ColorPair(EventExamenLight, EventExamenContainerLight),
    eventOtros = ColorPair(EventOtrosLight, EventOtrosContainerLight),
    heatmap = HeatmapLight,
    heatmapLabelIntense = HeatmapLabelIntenseLight,
    onBrandSubtle = OnBrandSubtleLight,
)

internal val DarkExtendedColors = PurpuraExtendedColors(
    statusPendiente = ColorPair(StatusPendienteDark, StatusPendienteContainerDark),
    statusRealizado = ColorPair(StatusRealizadoDark, StatusRealizadoContainerDark),
    statusAplazado = ColorPair(StatusAplazadoDark, StatusAplazadoContainerDark),
    eventCita = ColorPair(EventCitaDark, EventCitaContainerDark),
    eventJunta = ColorPair(EventJuntaDark, EventJuntaContainerDark),
    eventEntrega = ColorPair(EventEntregaDark, EventEntregaContainerDark),
    eventExamen = ColorPair(EventExamenDark, EventExamenContainerDark),
    eventOtros = ColorPair(EventOtrosDark, EventOtrosContainerDark),
    heatmap = HeatmapDark,
    heatmapLabelIntense = HeatmapLabelIntenseDark,
    onBrandSubtle = OnBrandSubtleDark,
)

/** Default = claro; [PurpuraTheme] inyecta el set correcto segun el modo. */
val LocalPurpuraColors = staticCompositionLocalOf { LightExtendedColors }
