package com.eddndev.purpura.ui.heatmap

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.theme.PurpuraTheme
import com.eddndev.purpura.ui.theme.Spacing

// Nivel a partir del cual la etiqueta del dia usa el token intenso. El umbral depende del tema para
// cumplir contraste AA: en CLARO la etiqueta intensa es blanca y contrasta desde el nivel 3; en
// OSCURO la etiqueta intensa es negra y solo alcanza AA sobre el mosaico mas brillante (nivel 4),
// asi que el nivel 3 oscuro se queda con onSurface (texto claro) que sobre #7B2AB8 da ~7:1.
private const val INTENSE_LABEL_LEVEL_LIGHT = 3
private const val INTENSE_LABEL_LEVEL_DARK = 4

// Forma de mosaico del mapa: esquinas suaves de tarjeta (NO pill), compartida por celdas, esqueleto y
// leyenda para que toda la escala se lea como la misma pieza.
private val TileShape = RoundedCornerShape(Spacing.md)

// Rejilla NO-lazy de 7 columnas: comparte el scroll del contenido (no anida scrolls). Cada fila es
// una semana; las celdas vacias rellenan para alinear el dia 1 y completar la ultima semana.
@Composable
internal fun HeatmapGrid(
    cells: List<HeatmapCell>,
    onDayClick: (HeatmapCell.Day) -> Unit,
) {
    val gridDesc = stringResource(R.string.heatmap_grid_desc)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = gridDesc },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        cells.chunked(MonthGrid.DAYS_PER_WEEK).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                week.forEach { cell ->
                    HeatmapCellTile(cell = cell, onDayClick = onDayClick, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// Una celda del mapa: dia tenido por intensidad o relleno vacio. El fondo se anima entre niveles 0..4
// con la curva estandar (220ms, FastOutSlowIn) para que el cambio de densidad se perciba al navegar.
@Composable
private fun HeatmapCellTile(
    cell: HeatmapCell,
    onDayClick: (HeatmapCell.Day) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (cell) {
        is HeatmapCell.Empty ->
            // Relleno: ocupa la columna sin pintar nada y sin semantica, asi no recibe foco de a11y.
            Box(modifier = modifier.aspectRatio(1f))

        is HeatmapCell.Day -> {
            val heatmap = PurpuraTheme.colors.heatmap
            val targetColor = heatmap[cell.level.coerceIn(0, heatmap.lastIndex)]
            val background by animateColorAsState(
                targetValue = targetColor,
                animationSpec = tween(220, easing = FastOutSlowInEasing),
                label = "heatmapCellColor",
            )
            // Texto: onSurface salvo en los niveles mas intensos, que usan el token intenso. El umbral
            // sube en oscuro (4) para no poner la etiqueta negra sobre un morado medio (fallaria AA).
            val intenseLevel = if (isSystemInDarkTheme()) INTENSE_LABEL_LEVEL_DARK else INTENSE_LABEL_LEVEL_LIGHT
            val textColor = if (cell.level >= intenseLevel) {
                PurpuraTheme.colors.heatmapLabelIntense
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            val borderColor = if (cell.isToday) MaterialTheme.colorScheme.primary else Color.Transparent
            // Descripcion por celda: fecha + conteo, fusionada para leerse como una sola unidad.
            val cellDesc = pluralStringResource(
                R.plurals.heatmap_day_count,
                cell.count,
                cell.count,
                EventDisplay.formatFullDate(cell.date),
            )

            Box(
                modifier = modifier
                    .aspectRatio(1f)
                    .clip(TileShape)
                    .background(background, TileShape)
                    .border(width = 2.dp, color = borderColor, shape = TileShape)
                    .clickable { onDayClick(cell) }
                    .semantics(mergeDescendants = true) { contentDescription = cellDesc },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = cell.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}

// Esqueleto de la rejilla en carga FRIA (sin cache): mismas filas/forma que el mapa real, tenidas en
// surfaceVariant. Evita el "spinner sobre vacio": el placeholder tiene la huella del contenido real.
@Composable
internal fun HeatmapSkeletonGrid(weeks: Int = 6) {
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(weeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                repeat(MonthGrid.DAYS_PER_WEEK) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(TileShape)
                            .background(placeholder, TileShape),
                    )
                }
            }
        }
    }
}

// Leyenda horizontal: "menos" [5 muestras nivel 0..4] "mas". Muestras cuadradas de 20dp con la forma
// suave de las celdas para que la escala se lea como las del mapa.
@Composable
internal fun HeatmapLegend() {
    val legendDesc = stringResource(R.string.heatmap_legend_desc)
    val heatmap = PurpuraTheme.colors.heatmap
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = legendDesc },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.heatmap_legend_less),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxs)) {
            heatmap.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(Spacing.sm))
                        .background(color),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.heatmap_legend_more),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Resumen del mes centrado: numero destacado (headlineSmall/primary) sobre la etiqueta secundaria
// ("eventos este mes"). Si el mes no tiene eventos, un aviso en linea en vez del conteo.
@Composable
internal fun MonthSummary(total: Int) {
    if (total == 0) {
        // Mes vacio: mensaje sobrio en linea (no un conteo en cero). Reusa la etiqueta generica
        // "Sin eventos"; ver sharedNeeds para un texto especifico del mes.
        Text(
            text = stringResource(R.string.calendar_day_empty_title),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        return
    }
    // La etiqueta sin numero se deriva del plural existente quitando el prefijo "%d ": permite separar
    // numero/etiqueta sin un string nuevo (ver sharedNeeds para una etiqueta dedicada).
    val label = pluralStringResource(R.plurals.heatmap_month_total, total, total)
        .removePrefix(total.toString())
        .trim()
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.xxs),
    ) {
        Text(
            text = total.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
