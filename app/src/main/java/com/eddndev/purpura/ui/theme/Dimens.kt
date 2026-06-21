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

    /** Margen horizontal estandar de las pantallas. */
    val screenH = 16.dp

    /** Separacion vertical entre items de una lista. */
    val item = 12.dp

    /** Separacion entre secciones de una pantalla. */
    val section = 24.dp

    /** Holgura inferior para que el ultimo item de una lista libre el FAB extendido. */
    val fabClearance = 96.dp
}
