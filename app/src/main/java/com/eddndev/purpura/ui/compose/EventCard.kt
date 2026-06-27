package com.eddndev.purpura.ui.compose

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.theme.CardShape
import com.eddndev.purpura.ui.theme.Elevation
import com.eddndev.purpura.ui.theme.PurpuraTheme
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Tarjeta de evento en Compose: columna "ticket" a la izquierda (tesela de fecha morada con mes/dia
 * + hora) y columna de contenido a la derecha (descripcion, contacto con icono, ubicacion con icono,
 * badges de tipo y estatus) con chevron de "abre Detalle". Compartida por Inicio, Consultar y el dia
 * del Calendario. El click navega al Detalle via [onClick].
 *
 * Acabado de marca: sombra suave tenida de morado ([Elevation.card] + shadowSpot/shadowAmbient) en
 * vez del negro duro de M3, y micro-escala al presionar (0.985) para dar feedback fisico.
 */
@Composable
fun EventCard(
    event: Event,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val a11y = stringResource(
        R.string.event_card_a11y,
        EventDisplay.formatFullDate(event.startsAt),
        EventDisplay.formatTime(event.startsAt),
        event.description,
        stringResource(EventDisplay.typeLabel(event.type)),
        stringResource(EventDisplay.statusLabel(event.status)),
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "cardPress",
    )
    Card(
        onClick = onClick,
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.level0),
        interactionSource = interaction,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = Elevation.card,
                shape = CardShape,
                spotColor = PurpuraTheme.colors.shadowSpot,
                ambientColor = PurpuraTheme.colors.shadowAmbient,
            )
            .clearAndSetSemantics { contentDescription = a11y },
    ) {
        Row(modifier = Modifier.padding(Spacing.cardPadding)) {
            DateTile(event)
            Spacer(Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val contact = event.contact.name
                if (contact.isNotBlank()) {
                    Spacer(Modifier.size(Spacing.xs))
                    MetaLine(Icons.Outlined.Person, contact)
                }
                val location = EventDisplay.locationSummary(LocalContext.current, event.location)
                if (location != null) {
                    Spacer(Modifier.size(Spacing.xs))
                    MetaLine(Icons.Outlined.Place, location)
                }
                Spacer(Modifier.size(Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    EventTypeBadge(event.type)
                    EventStatusBadge(event.status)
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(20.dp),
            )
        }
    }
}

/** Linea de metadato (contacto / ubicacion): icono guia de 14dp + texto onSurfaceVariant. */
@Composable
private fun MetaLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DateTile(event: Event) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(56.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.shapes.small,
                )
                .padding(vertical = Spacing.sm)
                .width(56.dp),
        ) {
            Text(
                text = EventDisplay.formatMonthAbbrev(event.startsAt).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 0.5.sp,
            )
            Text(
                text = EventDisplay.formatDayNumber(event.startsAt),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.size(Spacing.xs))
        Text(
            text = EventDisplay.formatTime(event.startsAt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
