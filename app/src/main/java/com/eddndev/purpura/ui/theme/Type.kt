package com.eddndev.purpura.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Tipografia de Purpura. Antes era un passthrough de la escala M3 (todo en W400), por lo que los
 * titulos grandes se veian delgados y la jerarquia era debil. Aqui se define una escala de marca
 * explicita: misma familia del sistema (sans), pero con PESOS asertivos (SemiBold en display /
 * headline / title) y tracking ajustado en el texto grande para un acabado de producto.
 *
 * Es el PUNTO UNICO de palanca tipografica: para inyectar una fuente propia, basta cambiar [Brand]
 * por un FontFamily de recursos y todos los estilos la heredan sin tocar ninguna pantalla.
 */
private val Brand = FontFamily.SansSerif

// Pesos de marca: titulos en SemiBold para dar peso visual; cuerpo/etiquetas en Medium/Normal.
private val Display = FontWeight.SemiBold
private val Heading = FontWeight.SemiBold
private val TitleWeight = FontWeight.SemiBold
private val Label = FontWeight.Medium

val PurpuraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Brand, fontWeight = Display,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.015).em,
    ),
    displayMedium = TextStyle(
        fontFamily = Brand, fontWeight = Display,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.01).em,
    ),
    displaySmall = TextStyle(
        fontFamily = Brand, fontWeight = Display,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.01).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = Brand, fontWeight = Heading,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Brand, fontWeight = Heading,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.005).em,
    ),
    headlineSmall = TextStyle(
        fontFamily = Brand, fontWeight = Heading,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Brand, fontWeight = TitleWeight,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Brand, fontWeight = TitleWeight,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.01.em,
    ),
    titleSmall = TextStyle(
        fontFamily = Brand, fontWeight = TitleWeight,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.005.em,
    ),
    bodyLarge = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Brand, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Brand, fontWeight = Label,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Brand, fontWeight = Label,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Brand, fontWeight = Label,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp,
    ),
)
