package com.eddndev.purpura.ui.about

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.theme.Elevation
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.PurpuraShapes
import com.eddndev.purpura.ui.theme.PurpuraTheme
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Acerca de (REQ-AUTH-004) en Compose: grupo de identidad con peso arriba (marca + nombre + version +
 * creditos) sobre [PurpuraScreen] con flecha atras. NO se centra todo en vertical: la identidad se
 * ancla arriba con un respiro de seccion y el cierre de sesion se empuja al fondo con un Spacer de
 * peso, para que la jerarquia lea "marca primero, accion al final". El logout es de baja enfasis pero
 * sigue accesible; la vuelta a Auth la hace MainActivity al observar la sesion en null (no esta vista).
 */
@Composable
fun AboutScreen(
    versionName: String,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    PurpuraScreen(
        title = stringResource(R.string.title_about),
        modifier = modifier,
        onBack = onBack,
    ) { innerPadding ->
        // Columna limitada a 360dp y centrada (legible en tablets); el contenido es corto, sin scroll.
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxHeight()
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top,
            ) {
                // Respiro superior de seccion: separa la marca del app bar sin centrar todo el bloque.
                Spacer(Modifier.height(Spacing.section))
                BrandMark()
                Spacer(Modifier.height(Spacing.xl))
                Text(
                    text = stringResource(R.string.about_app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    // Version real desde BuildConfig (el tag del release la inyecta); no hardcodear.
                    text = stringResource(R.string.account_version, versionName),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = stringResource(R.string.about_credits),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                // Empuja el logout al fondo: la accion no compite con la identidad.
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { showLogoutDialog = true },
                    shape = Pill,
                    // Baja enfasis tenida de peligro: accesible pero sin gritar como un boton primario.
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.size(Spacing.sm))
                    Text(stringResource(R.string.about_logout))
                }
                Spacer(Modifier.height(Spacing.lg))
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text(stringResource(R.string.about_logout_title)) },
            text = { Text(stringResource(R.string.about_logout_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) { Text(stringResource(R.string.about_logout_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.about_logout_cancel))
                }
            },
        )
    }
}

/**
 * Pastilla de marca: logo sobre superficie primaria con esquina extraLarge (28dp) y sombra suave
 * tenida de morado (spotColor de la marca, igual que las cards). Reveal de una sola vez al entrar
 * (escala 0.92->1 + fade) para que la identidad "aterrice" en lugar de aparecer de golpe. El logo va
 * a fillMaxSize: el foreground adaptativo ya trae su zona segura, sin padding interno artificial.
 */
@Composable
private fun BrandMark() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing))
    }

    Surface(
        shape = PurpuraShapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .size(96.dp)
            .graphicsLayer {
                alpha = reveal.value
                val s = 0.92f + 0.08f * reveal.value
                scaleX = s
                scaleY = s
            }
            // Sombra de marca: spotColor morado. La elevacion 0 de Surface evita una segunda sombra dura.
            .shadow(
                elevation = Elevation.cardRaised,
                shape = PurpuraShapes.extraLarge,
                spotColor = PurpuraTheme.colors.shadowSpot,
                ambientColor = PurpuraTheme.colors.shadowAmbient,
            ),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            // Decorativo: el nombre de la app justo debajo ya lo anuncia (evita doble lectura en TalkBack).
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
