package com.eddndev.purpura.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColors: ColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainer = SurfaceContainerLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainer = SurfaceContainerDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

/**
 * Tema raiz de Purpura para Compose. Porta el design system XML: roles M3 (claro/oscuro), forma pill
 * global ([PurpuraShapes] + [Pill] por componente), tipografia M3 y los colores semanticos via
 * [LocalPurpuraColors]. SIN dynamic color: la marca es mono-morado disciplinado por requerimiento.
 *
 * Accede a los tokens extendidos con [PurpuraTheme.colors] dentro de cualquier composable.
 */
@Composable
fun PurpuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        // El context del ComposeView puede ser un ContextWrapper (Hilt envuelve el del Fragment en un
        // FragmentContextWrapper): hay que desenvolver hasta el Activity, NUNCA castear directo (eso
        // crasheaba con ClassCastException en todas las pantallas).
        val activity = view.context.findActivity()
        if (activity != null) {
            SideEffect {
                // Edge-to-edge: el contenido va bajo las barras; aqui solo se ajusta el color de los
                // iconos de la status bar para que contrasten con el fondo del tema.
                WindowCompat.getInsetsController(activity.window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPurpuraColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PurpuraTypography,
            shapes = PurpuraShapes,
            content = content,
        )
    }
}

/** Accesor de los tokens extendidos de Purpura, al estilo `MaterialTheme.colorScheme`. */
object PurpuraTheme {
    val colors: PurpuraExtendedColors
        @Composable
        get() = LocalPurpuraColors.current
}

// Desenvuelve la cadena de ContextWrapper hasta encontrar el Activity (o null). Necesario porque el
// context de un ComposeView dentro de un Fragment con Hilt es un FragmentContextWrapper, no el Activity.
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
