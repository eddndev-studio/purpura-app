package com.eddndev.purpura.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.EmptyState
import com.eddndev.purpura.ui.compose.EventCard
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.compose.SkeletonList
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalTime

/**
 * Inicio (REQ-HOME-001/002) en Compose. EXEMPLAR del rediseno: barra compacta con saludo+fecha como
 * subtitulo, lista de eventos [hoy, hoy+4] con ritmo de 12dp, pull-to-refresh, y FAB extendido que se
 * compacta al hacer scroll. Las tres fases (cargando/vacio/contenido) se cruzan con Crossfade: en
 * frio se muestran esqueletos (no un spinner sobre lista vacia) y el vacio ofrece una accion directa.
 */
private enum class HomePhase { Loading, Empty, Content }

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onAddEvent: () -> Unit,
    onEventClick: (Event) -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val fabExpanded by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 80
        }
    }

    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    val phase = when {
        state.events.isEmpty() && state.isLoading -> HomePhase.Loading
        state.isEmpty -> HomePhase.Empty
        else -> HomePhase.Content
    }

    PurpuraScreen(
        title = stringResource(R.string.title_home),
        modifier = modifier,
        subtitle = greetingWithDate(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddEvent,
                expanded = fabExpanded,
                icon = {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.fab_add_event))
                },
                text = { Text(stringResource(R.string.fab_add_event)) },
                shape = Pill,
            )
        },
    ) { innerPadding ->
        Crossfade(
            targetState = phase,
            animationSpec = tween(200),
            label = "homePhase",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { current ->
            when (current) {
                HomePhase.Loading -> SkeletonList(
                    count = 4,
                    modifier = Modifier.padding(
                        horizontal = Spacing.screenH,
                        vertical = Spacing.sm,
                    ),
                )

                HomePhase.Empty -> EmptyState(
                    icon = Icons.Outlined.CalendarMonth,
                    title = stringResource(R.string.home_empty_title),
                    body = stringResource(R.string.placeholder_home),
                    action = {
                        Button(onClick = onAddEvent, shape = Pill) {
                            Text(stringResource(R.string.empty_create_event))
                        }
                    },
                )

                HomePhase.Content -> PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    EventList(state.events, listState, onEventClick)
                }
            }
        }
    }
}

@Composable
private fun EventList(
    events: List<Event>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onEventClick: (Event) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = Spacing.screenH,
            end = Spacing.screenH,
            top = Spacing.sm,
            bottom = Spacing.fabClearance,
        ),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.item),
    ) {
        item {
            SectionHeader(
                text = stringResource(R.string.home_section_upcoming),
                modifier = Modifier.padding(top = Spacing.sm),
                trailing = {
                    Text(
                        text = events.size.toString(),
                        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
        items(events, key = { it.id }) { event ->
            EventCard(
                event = event,
                onClick = { onEventClick(event) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

// Saludo por hora del dia + fecha de hoy, como subtitulo del app bar. LocalTime.now en composicion es
// suficiente: el saludo no necesita ser reactivo dentro de la misma sesion de pantalla.
@Composable
private fun greetingWithDate(): String {
    val hour = remember { LocalTime.now().hour }
    val greetingRes = when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..18 -> R.string.home_greeting_afternoon
        else -> R.string.home_greeting_evening
    }
    return stringResource(greetingRes) + " - " + EventDisplay.formatToday()
}
