package com.eddndev.purpura.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
 * UNICA AUTORIDAD DE INSETS (corrige el "hueco molesto encima del titulo"):
 * - El `TopAppBar` COMPACTO consume el inset de la status bar (windowInsets por defecto) y deja el
 *   titulo centrado a pocos dp de la barra de estado. Antes los hubs usaban un `LargeTopAppBar`
 *   colapsable cuyo titulo queda anclado ABAJO, dejando una banda vacia arriba que se leia como
 *   padding muerto: eso era el "hueco". Ahora NO hay banda: una sola barra compacta para todas.
 * - `contentWindowInsets = WindowInsets(0)`: el Scaffold no agrega insets al contenido. El top lo
 *   posee la barra; el bottom lo posee el `BottomNavigationView` (en los 4 destinos top-level) o el
 *   [bottomBar] (que ya libera la gesture bar via navigationBarsPadding). Asi se elimina cualquier
 *   doble conteo de inset.
 * - El contenedor de la barra se funde con el fondo en reposo y sube a `surfaceContainer` al hacer
 *   scroll, para que la barra "gane" su espacio sin flotar.
 *
 * @param subtitle segunda linea opcional bajo el titulo (saludo/fecha en Inicio, etc.).
 * @param bottomBar barra inferior fija (p. ej. CTA de Detalle/Anadir). Ya libera la gesture bar.
 * @param large OBSOLETO: se mantiene por compatibilidad de firma; hoy se ignora (todas las pantallas
 *   usan la misma barra compacta). No usar en codigo nuevo.
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
    @Suppress("UNUSED_PARAMETER") large: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    // enterAlways: la barra se oculta al hacer scroll hacia abajo y reaparece al subir (inmersivo en
    // listas), sin la banda vacia del bar colapsable grande.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
                style = MaterialTheme.typography.titleLarge,
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
        },
        floatingActionButton = floatingActionButton,
        // Si hay bottomBar, libera la gesture bar (en Detalle/formularios el CTA no queda detras de la
        // barra del sistema). Si NO hay, queda en {} para no reservar inset inferior en los 4 destinos
        // top-level, donde el BottomNavigationView (XML) ya posee ese inset.
        bottomBar = {
            if (bottomBar != null) {
                androidx.compose.foundation.layout.Box(Modifier.navigationBarsPadding()) { bottomBar() }
            }
        },
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content,
    )
}
