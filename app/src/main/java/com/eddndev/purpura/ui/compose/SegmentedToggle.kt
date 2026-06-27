package com.eddndev.purpura.ui.compose

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eddndev.purpura.ui.theme.Pill

/**
 * Control segmentado (single choice) de marca. Lo usa el toggle "Mes / Calor" del Calendario y
 * cualquier seleccion exclusiva corta. Envuelve el segmented button de M3 para uniformar forma.
 *
 * @param options lista de valores; [labelOf] da su etiqueta visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    // @Composable para poder resolver stringResource(...) en las etiquetas desde el sitio de llamada.
    labelOf: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                // baseShape = Pill: las puntas exteriores quedan en capsula (50%), fiel a la marca.
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                    baseShape = Pill,
                ),
                // Sin el check de seleccion: el color tonal ya comunica el estado y deja mas aire.
                icon = {},
            ) {
                Text(labelOf(option))
            }
        }
    }
}
