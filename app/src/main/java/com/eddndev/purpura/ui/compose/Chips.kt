package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.theme.ColorPair
import com.eddndev.purpura.ui.theme.Pill

/**
 * Badge pill de solo lectura: texto fuerte sobre su container. Es el equivalente Compose del
 * `bg_pill_badge` + tint del `item_event.xml`. Para el chip seleccionable (filtros, detalle) se usa
 * el `FilterChip` de M3 directamente.
 */
@Composable
fun PurpuraBadge(
    text: String,
    colors: ColorPair,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.strong,
        modifier = modifier
            .background(colors.container, Pill)
            .padding(horizontal = 10.dp, vertical = 3.dp),
    )
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
    )
}
