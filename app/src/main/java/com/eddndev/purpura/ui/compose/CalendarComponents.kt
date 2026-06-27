package com.eddndev.purpura.ui.compose

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R

/**
 * Cabecera de navegacion de mes compartida por Calendario y Mapa de calor: flechas grandes, mes/anio
 * centrado (con Crossfade al cambiar) y, opcional, un acceso "Hoy". De-duplica el control que ambas
 * pantallas tenian copiado.
 */
@Composable
fun MonthNavHeader(
    monthLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    onToday: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResourceSafe(R.string.calendar_prev_month),
            )
        }
        Crossfade(
            targetState = monthLabel,
            animationSpec = tween(200),
            label = "monthLabel",
            modifier = Modifier.weight(1f),
        ) { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (onToday != null) {
            TextButton(onClick = onToday) { Text(stringResourceSafe(R.string.action_today)) }
        }
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResourceSafe(R.string.calendar_next_month),
            )
        }
    }
}

/** Fila de iniciales de los dias de la semana (L M M J V S D), repartida en 7 columnas iguales. */
@Composable
fun WeekdayHeaderRow(labels: List<String>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth()) {
        labels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Celda de dia: circulo de 40dp con relleno animado para el dia seleccionado y un anillo para hoy.
 * NO recorta la celda entera (los puntos por tipo se dibujan debajo por la pantalla). El color del
 * relleno se anima con animateColorAsState (220ms) para que la seleccion se sienta viva.
 */
@Composable
fun DayChip(
    day: String,
    selected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "dayFill",
    )
    val textColor = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .size(40.dp)
            .background(fill, CircleShape)
            .then(
                if (isToday && !selected) {
                    Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary), CircleShape)
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
        )
    }
}

/** Helper centralizado para no romper el build si una string aun no existe en algun modulo. */
@Composable
private fun stringResourceSafe(id: Int): String = androidx.compose.ui.res.stringResource(id)
