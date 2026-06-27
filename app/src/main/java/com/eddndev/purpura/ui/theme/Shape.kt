package com.eddndev.purpura.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas de Purpura (REQ-NFR-006).
 *
 * - [Pill]: capsula fully-rounded (50%) para TODO lo interactivo (botones, chips, campos, search
 *   bar, FAB). Es la marca de la estetica de Purpura.
 * - [PurpuraShapes]: radios para SUPERFICIES no interactivas (cards, sheets, dialogos, celdas de
 *   calendario/heatmap). Estas NO usan capsula.
 */
val Pill = RoundedCornerShape(percent = 50)

val PurpuraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Forma de las hojas modales (ModalBottomSheet): solo esquinas superiores, 28dp. */
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Alias semantico de la forma de card (16dp) para no repetir el literal en los sitios de llamada. */
val CardShape = RoundedCornerShape(16.dp)
