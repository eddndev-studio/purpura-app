package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.CardShape
import com.eddndev.purpura.ui.theme.Elevation
import com.eddndev.purpura.ui.theme.PurpuraTheme
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Presentacion pulida de una ubicacion en mapa. Resuelve el "look crudo" de Google Maps: encapsula el
 * [LiteLocationMap] (estilo de marca + punto morado) en una card de esquinas redondeadas, pinta un
 * placeholder detras (para que nunca parpadee en blanco mientras carga el mapa), superpone un
 * degradado inferior con la direccion y, opcional, un boton "Abrir en Maps". Sombra suave de marca.
 *
 * @param height altura del mapa: [Spacing.mapCard] (180dp) en formularios/listas; mayor (200-220dp)
 *   como "hero" en el Detalle.
 */
@Composable
fun MapCard(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    label: String? = null,
    height: Dp = Spacing.mapCard,
    onOpenExternal: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = Elevation.cardRaised,
                shape = CardShape,
                spotColor = PurpuraTheme.colors.shadowSpot,
                ambientColor = PurpuraTheme.colors.shadowAmbient,
            )
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        // Placeholder bajo el mapa: icono de ubicacion centrado mientras el MapView carga su bitmap.
        Icon(
            imageVector = Icons.Outlined.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.Center).height(40.dp),
        )

        LiteLocationMap(lat = lat, lng = lng, modifier = Modifier.fillMaxSize())

        if (!label.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                PurpuraTheme.colors.mapOverlayTop,
                                PurpuraTheme.colors.mapOverlayBottom,
                            ),
                        ),
                    )
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    // Texto claro fijo: va sobre el degradado oscuro en claro y oscuro.
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (onOpenExternal != null) {
            FilledTonalIconButton(
                onClick = onOpenExternal,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(R.string.map_open_external),
                )
            }
        }
    }
}
