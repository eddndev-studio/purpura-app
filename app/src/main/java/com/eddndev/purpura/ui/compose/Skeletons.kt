package com.eddndev.purpura.ui.compose

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.ui.theme.CardShape
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Esqueletos de carga (shimmer). Sustituyen el "spinner sobre lista vacia": en frio se muestran
 * placeholders con la MISMA huella que el contenido real, que se disuelven (Crossfade en la
 * pantalla) hacia los datos. Es el patron que diferencia un producto pulido de una demo.
 */

/** Brush de shimmer: gradiente que se desplaza ~1100ms sobre las superficies de placeholder. */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Restart),
        label = "shimmerTranslate",
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceBright
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(translate - 300f, 0f),
        end = Offset(translate, 0f),
    )
}

@Composable
private fun ShimmerBlock(
    modifier: Modifier,
    brush: Brush,
    shape: Shape = RoundedCornerShape(Spacing.sm),
) {
    Spacer(modifier = modifier.background(brush, shape))
}

/** Placeholder con la huella de [EventCard]: tesela de fecha + lineas de titulo/meta/badges. */
@Composable
fun EventCardSkeleton(modifier: Modifier = Modifier, brush: Brush = shimmerBrush()) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, CardShape)
            .padding(Spacing.cardPadding),
    ) {
        ShimmerBlock(Modifier.size(width = 56.dp, height = 64.dp), brush)
        Spacer(Modifier.width(Spacing.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            ShimmerBlock(Modifier.fillMaxWidth(0.8f).height(16.dp), brush)
            ShimmerBlock(Modifier.fillMaxWidth(0.5f).height(12.dp), brush)
            ShimmerBlock(Modifier.fillMaxWidth(0.6f).height(12.dp), brush)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ShimmerBlock(Modifier.size(width = 56.dp, height = 18.dp), brush)
                ShimmerBlock(Modifier.size(width = 64.dp, height = 18.dp), brush)
            }
        }
    }
}

/** Lista de [count] esqueletos de card, con el ritmo de lista estandar (12dp). Comparte un brush. */
@Composable
fun SkeletonList(
    count: Int = 4,
    modifier: Modifier = Modifier,
) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.item),
    ) {
        repeat(count) { EventCardSkeleton(brush = brush) }
    }
}

/** Esqueleto del Detalle: cabecera (titulo/fecha) + bloque de mapa + grupo de filas de info. */
@Composable
fun DetailSkeleton(modifier: Modifier = Modifier) {
    val brush = shimmerBrush()
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screenH, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        ShimmerBlock(Modifier.fillMaxWidth(0.7f).height(28.dp), brush)
        ShimmerBlock(Modifier.fillMaxWidth(0.4f).height(16.dp), brush)
        ShimmerBlock(Modifier.fillMaxWidth().height(180.dp), brush, CardShape)
        repeat(3) {
            ShimmerBlock(Modifier.fillMaxWidth().height(56.dp), brush, CardShape)
        }
    }
}
