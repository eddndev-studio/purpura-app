package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.theme.ColorPair
import com.eddndev.purpura.ui.theme.Pill

/**
 * Badge pill de solo lectura: texto fuerte sobre su container. Es el equivalente Compose del
 * `bg_pill_badge` + tint del `item_event.xml`. Con [showDot] antepone un punto de 6dp del color
 * fuerte, util para reforzar el estatus de forma independiente del matiz (accesibilidad).
 */
@Composable
fun PurpuraBadge(
    text: String,
    colors: ColorPair,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(colors.container, Pill)
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (showDot) {
            androidx.compose.foundation.layout.Box(
                Modifier.size(6.dp).background(colors.strong, CircleShape),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = colors.strong,
        )
    }
}

@Composable
fun EventTypeBadge(type: EventType, modifier: Modifier = Modifier) {
    PurpuraBadge(
        text = stringResource(EventDisplay.typeLabel(type)),
        colors = colorsFor(type),
        modifier = modifier,
    )
}

@Composable
fun EventStatusBadge(status: EventStatus, modifier: Modifier = Modifier) {
    PurpuraBadge(
        text = stringResource(EventDisplay.statusLabel(status)),
        colors = colorsFor(status),
        modifier = modifier,
        showDot = true,
    )
}

/**
 * Chip de filtro de marca: capsula pill con check al seleccionar y colores de marca
 * (primaryContainer) en vez del secondaryContainer por defecto de M3. Lo usan Consultar y
 * Anadir/Editar para Tipo/Estatus/Recordatorio/Periodo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurpuraFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        shape = Pill,
        leadingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = modifier,
    )
}
