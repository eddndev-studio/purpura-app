package com.eddndev.purpura.ui.theme

import androidx.compose.material3.Typography

/**
 * Tipografia de Purpura. Hoy es passthrough de la escala M3 (igual que el `TextAppearance.Purpura.*`
 * del tema XML: hereda M3 identico). Es el PUNTO UNICO de palanca de marca: para inyectar una fuente
 * propia, basta definir un [androidx.compose.ui.text.font.FontFamily] aqui y aplicarlo a los estilos,
 * sin tocar ninguna pantalla.
 */
val PurpuraTypography = Typography()
