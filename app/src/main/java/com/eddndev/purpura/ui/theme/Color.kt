package com.eddndev.purpura.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color de Purpura portados VERBATIM desde el sistema de diseno
 * (`specs/02-design-system.md`, `res/values/colors.xml` y `res/values-night/colors.xml`).
 *
 * Los roles M3 alimentan `lightColorScheme`/`darkColorScheme` en [com.eddndev.purpura.ui.theme.PurpuraTheme].
 * Los colores semanticos (estatus, tipo de evento, heatmap) NO son roles M3: se exponen como
 * [PurpuraExtendedColors] via CompositionLocal. La fuente de verdad de los hex es el XML (la noche
 * "high-end" usa #C68BFF como primary, que difiere de la tabla de la spec).
 */

// ---------------------------------------------------------------------------
// Roles M3 - Tema claro
// ---------------------------------------------------------------------------
val PrimaryLight = Color(0xFF6A1B9A)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFEBDCF2)
val OnPrimaryContainerLight = Color(0xFF4A148C)
val SecondaryLight = Color(0xFF7B1FA2)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFE9D7F2)
val OnSecondaryContainerLight = Color(0xFF3A0E55)
val TertiaryLight = Color(0xFF9A2D63)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFD9E8)
val OnTertiaryContainerLight = Color(0xFF3E0021)
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)
val BackgroundLight = Color(0xFFF3E5F5)
val OnBackgroundLight = Color(0xFF1D1A20)
val SurfaceLight = Color(0xFFFFFBFE)
val OnSurfaceLight = Color(0xFF1D1A20)
val SurfaceVariantLight = Color(0xFFE7E0EB)
val OnSurfaceVariantLight = Color(0xFF49454F)
val SurfaceContainerLight = Color(0xFFF4EDF7)
val OutlineLight = Color(0xFF7A757F)
val OutlineVariantLight = Color(0xFFCAC4CF)

// ---------------------------------------------------------------------------
// Roles M3 - Tema oscuro (noche "high-end")
// ---------------------------------------------------------------------------
val PrimaryDark = Color(0xFFC68BFF)
val OnPrimaryDark = Color(0xFF2A0A45)
val PrimaryContainerDark = Color(0xFF5A2D86)
val OnPrimaryContainerDark = Color(0xFFEFDBFF)
val SecondaryDark = Color(0xFFD8B9E6)
val OnSecondaryDark = Color(0xFF3A0E55)
val SecondaryContainerDark = Color(0xFF5A3A6E)
val OnSecondaryContainerDark = Color(0xFFEFDBFF)
val TertiaryDark = Color(0xFFFFB0CC)
val OnTertiaryDark = Color(0xFF5E1136)
val TertiaryContainerDark = Color(0xFF7B2A4E)
val OnTertiaryContainerDark = Color(0xFFFFD9E8)
val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)
val BackgroundDark = Color(0xFF100A18)
val OnBackgroundDark = Color(0xFFECE2F5)
val SurfaceDark = Color(0xFF1A1322)
val OnSurfaceDark = Color(0xFFECE2F5)
val SurfaceVariantDark = Color(0xFF463B55)
val OnSurfaceVariantDark = Color(0xFFD0C4DE)
val SurfaceContainerDark = Color(0xFF241A30)
val OutlineDark = Color(0xFF988CA6)
val OutlineVariantDark = Color(0xFF463B55)

// ---------------------------------------------------------------------------
// Semanticos: estatus de evento (semaforo, deliberadamente no morado)
// ---------------------------------------------------------------------------
val StatusPendienteLight = Color(0xFF5E4900)
val StatusPendienteContainerLight = Color(0xFFFFF1C2)
val StatusRealizadoLight = Color(0xFF1B5E20)
val StatusRealizadoContainerLight = Color(0xFFC8E6C9)
val StatusAplazadoLight = Color(0xFF5F6368)
val StatusAplazadoContainerLight = Color(0xFFE0E2E5)

val StatusPendienteDark = Color(0xFFF2C94C)
val StatusPendienteContainerDark = Color(0xFF4A3B00)
val StatusRealizadoDark = Color(0xFFA5D6A7)
val StatusRealizadoContainerDark = Color(0xFF143D17)
val StatusAplazadoDark = Color(0xFFC4C7CC)
val StatusAplazadoContainerDark = Color(0xFF3A3D42)

// ---------------------------------------------------------------------------
// Semanticos: tipo de evento
// ---------------------------------------------------------------------------
val EventCitaLight = Color(0xFF6A1B9A)
val EventCitaContainerLight = Color(0xFFEBDCF2)
val EventJuntaLight = Color(0xFF3F51B5)
val EventJuntaContainerLight = Color(0xFFDDE1F9)
val EventEntregaLight = Color(0xFF00695C)
val EventEntregaContainerLight = Color(0xFFC8EFE9)
val EventExamenLight = Color(0xFFA1144B)
val EventExamenContainerLight = Color(0xFFFAD7E3)
val EventOtrosLight = Color(0xFF5F5A66)
val EventOtrosContainerLight = Color(0xFFE7E0EB)

val EventCitaDark = Color(0xFFD0BCFF)
val EventCitaContainerDark = Color(0xFF3A1A52)
val EventJuntaDark = Color(0xFFB6C4FF)
val EventJuntaContainerDark = Color(0xFF27315F)
val EventEntregaDark = Color(0xFF7DDBCB)
val EventEntregaContainerDark = Color(0xFF0E3A33)
val EventExamenDark = Color(0xFFFFB0CC)
val EventExamenContainerDark = Color(0xFF54122F)
val EventOtrosDark = Color(0xFFCAC4D0)
val EventOtrosContainerDark = Color(0xFF3A363F)

// ---------------------------------------------------------------------------
// Semanticos: heatmap (5 niveles) + etiqueta sobre el mosaico mas intenso
// ---------------------------------------------------------------------------
val HeatmapLight = listOf(
    Color(0xFFF3E9F7),
    Color(0xFFD7B8E8),
    Color(0xFFB57EDC),
    Color(0xFF8E33C4),
    Color(0xFF5C0F8B),
)
val HeatmapDark = listOf(
    Color(0xFF2A2630),
    Color(0xFF3F2A57),
    Color(0xFF5A2A87),
    Color(0xFF7B2AB8),
    Color(0xFFA85CE0),
)
val HeatmapLabelIntenseLight = Color(0xFFFFFFFF)
val HeatmapLabelIntenseDark = Color(0xFF000000)

// Texto secundario sobre superficie de marca (cabecera del drawer / bajo toolbar)
val OnBrandSubtleLight = Color(0xFFE8D5F0)
val OnBrandSubtleDark = Color(0xFFEFDBFF)
