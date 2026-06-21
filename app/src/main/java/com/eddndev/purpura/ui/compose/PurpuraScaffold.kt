package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.eddndev.purpura.R

/**
 * Scaffold estandar de Purpura: encapsula TODO el cableado del app bar para que cada pantalla solo
 * declare su titulo y su contenido, y todas se vean coherentes.
 *
 * - Edge-to-edge: el TopAppBar consume el inset de la status bar (windowInsets por defecto); el
 *   contenido recibe [PaddingValues] correctos. La Activity ya no pinta toolbar XML.
 * - [large] = LargeTopAppBar colapsable (pantallas hub: Inicio, Cuenta). false = TopAppBar normal.
 * - El comportamiento de scroll y la conexion nestedScroll se crean y enlazan aqui dentro.
 * - Colores: contenedor transparente expandido (se funde con el fondo) -> surfaceContainer al
 *   hacer scroll, para que el titulo no flote sobre la lista.
 *
 * Los agentes de rediseno usan SOLO este scaffold; no recrean Scaffold/TopAppBar a mano.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurpuraScreen(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior = if (large) {
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    } else {
        TopAppBarDefaults.enterAlwaysScrollBehavior()
    }

    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                )
            }
        }
    }
    val titleContent: @Composable () -> Unit = {
        Column {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            if (large) {
                LargeTopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            } else {
                TopAppBar(
                    title = titleContent,
                    navigationIcon = navigationIcon,
                    actions = actions,
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            }
        },
        floatingActionButton = floatingActionButton,
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}
