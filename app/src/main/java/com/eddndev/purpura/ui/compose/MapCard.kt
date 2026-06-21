package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R

/**
 * Presentacion pulida de una ubicacion en mapa. Resuelve el "look crudo" de Google Maps:
 * encapsula el [LiteLocationMap] (estilo de marca + marcador morado) en una card de esquinas
 * redondeadas (recorta los bordes del mapa), superpone un degradado inferior con la direccion y, de
 * forma opcional, un boton "Abrir en Maps". No expone controles nativos del mapa.
 */
@Composable
fun MapCard(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    label: String? = null,
    height: androidx.compose.ui.unit.Dp = 180.dp,
    onOpenExternal: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(height)) {
            LiteLocationMap(lat = lat, lng = lng, modifier = Modifier.fillMaxSize())

            if (!label.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0f),
                                    MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                                ),
                            ),
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        // Texto claro fijo: va sobre el degradado oscuro (scrim) en claro y oscuro.
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
                        .padding(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.map_open_external),
                    )
                }
            }
        }
    }
}
