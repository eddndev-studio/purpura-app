package com.eddndev.purpura.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.eddndev.purpura.ui.theme.PurpuraTheme

/**
 * Puente de la migracion incremental: cada Fragment devuelve en `onCreateView` un [ComposeView] que
 * monta su pantalla Compose bajo [PurpuraTheme]. El Fragment conserva los launchers de ActivityResult,
 * permisos en runtime y FragmentResult, y pasa callbacks al composable. Asi se migra pantalla por
 * pantalla manteniendo Navigation Component y el build verde en cada paso.
 */
fun Fragment.purpuraComposeView(content: @Composable () -> Unit): ComposeView =
    ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            PurpuraTheme {
                content()
            }
        }
    }
