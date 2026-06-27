package com.eddndev.purpura.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.BrandIconBadge
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

// Piezas de presentacion de Auth extraidas aqui para mantener AuthScreen.kt bajo el limite de LOC.
// Son internas (no privadas) porque se consumen desde AuthScreen.kt en el mismo paquete.

/**
 * Branding centrado: pastilla de marca de la fundacion + titulo y subtitulo. El titulo usa
 * onBackground (NO primary): el color primario queda reservado para el badge y el CTA, dando una
 * sola jerarquia de acento por pantalla.
 */
@Composable
internal fun AuthBranding(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // BrandIconBadge toma un ImageVector (no painter), igual que el exemplar Inicio.
        BrandIconBadge(icon = Icons.Outlined.CalendarMonth)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.title_auth),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        // Titulo -> subtitulo: proximidad estrecha para leerse como un bloque (Spacing.sm).
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Alterna login/registro como afordancia inline demotada (no un boton de ancho completo): asi el CTA
 * primario manda la jerarquia. La etiqueta cruza con AnimatedContent fade-through al cambiar de modo.
 */
@Composable
internal fun AuthModeToggle(
    isRegister: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onToggle, enabled = enabled, shape = Pill) {
            AnimatedContent(
                targetState = isRegister,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                label = "authToggleLabel",
            ) { register ->
                Text(
                    text = stringResource(
                        if (register) R.string.auth_toggle_to_login else R.string.auth_toggle_to_register,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Divisor "o" real: dos lineas con la etiqueta centrada (separa el formulario de Google). */
@Composable
internal fun AuthDivider(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

/**
 * Boton de Google (outline). Lleva el glifo oficial con tint Unspecified para conservar sus colores
 * de marca, separado del texto por Spacing.sm. El intent lo lanza el callback del Fragment.
 */
@Composable
internal fun AuthGoogleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = Pill,
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = stringResource(R.string.auth_google_icon_desc),
            tint = Color.Unspecified,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(Spacing.sm))
        Text(stringResource(R.string.auth_google))
    }
}
