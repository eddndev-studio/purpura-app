package com.eddndev.purpura.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Escala de espaciado de Purpura (multiplos de 4). Punto unico para que todas las pantallas
 * respiren igual: margen horizontal de pantalla, separacion entre secciones, entre items, etc.
 * Espeja `res/values/dimens.xml` para mantener paridad XML/Compose durante la migracion.
 */
object Spacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 48.dp

    /** Margen horizontal estandar de las pantallas. */
    val screenH = 16.dp

    /** Separacion vertical entre items de una lista. */
    val item = 12.dp

    /** Separacion entre secciones de una pantalla. */
    val section = 24.dp

    /** Padding interno estandar de una card. */
    val cardPadding = 16.dp

    /** Area minima de toque accesible (M3). */
    val minTouchTarget = 48.dp

    /** Altura por defecto de un MapCard de lista/formulario. */
    val mapCard = 180.dp

    /** Holgura inferior para que el ultimo item de una lista libre el FAB extendido. */
    val fabClearance = 96.dp
}

/**
 * Escala de elevacion de Purpura. Punto unico para las sombras: las cards y el MapCard usan sombras
 * suaves tenidas de morado (ver [com.eddndev.purpura.ui.theme.PurpuraExtendedColors.shadowSpot]), no
 * el negro duro por defecto. Evita que cada pantalla invente su propia .shadow().
 */
object Elevation {
    val level0 = 0.dp
    val card = 1.dp
    val cardRaised = 3.dp
    val bottomBar = 3.dp
    val fab = 6.dp
    val sheet = 1.dp
    val dialog = 6.dp
    val menu = 2.dp
}
