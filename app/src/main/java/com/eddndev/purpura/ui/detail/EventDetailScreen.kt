package com.eddndev.purpura.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.ErrorState
import com.eddndev.purpura.ui.compose.EventStatusBadge
import com.eddndev.purpura.ui.compose.EventTypeBadge
import com.eddndev.purpura.ui.compose.InfoRow
import com.eddndev.purpura.ui.compose.LoadingState
import com.eddndev.purpura.ui.compose.MapCard
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Detalle de un evento (REQ-QUERY-007..013) en Compose, sobre [PurpuraScreen] con flecha atras y
 * accion de editar en el app bar. Cabecera con fecha completa, hora destacada y badges; secciones de
 * info (contacto, ubicacion, recordatorio) con [InfoRow] y un [MapCard] pulido cuando hay coordenadas;
 * el estatus se cambia con un [SegmentedToggle]. Acciones al pie: editar (primario) y eliminar (peligro)
 * con confirmacion. El fallo de carga usa [ErrorState] con reintentar; el error sobre un evento ya
 * visible es un aviso de un solo uso (snackbar). La navegacion vive en el Fragment via callbacks.
 *
 * [onOpenMap] dispara la affordance "Abrir en Maps" del [MapCard] (spec §7); la pantalla es pura, asi
 * que el Intent lo resuelve el Fragment. Si es null, el boton no se muestra (sin coordenadas o sin
 * cableado todavia).
 */
@Composable
fun EventDetailScreen(
    state: DetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onChangeStatus: (EventStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleted: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMap: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Senal de un solo uso: al borrarse, el Fragment regresa atras.
    LaunchedEffect(state.deleted) {
        if (state.deleted) onDeleted()
    }

    // El error sobre un evento visible se muestra como snackbar; el fallo de carga usa el estado de
    // error con reintentar.
    state.errorRes?.let { messageRes ->
        if (state.event != null) {
            val message = stringResource(messageRes)
            LaunchedEffect(messageRes, message) {
                snackbarHostState.showSnackbar(message)
                onErrorShown()
            }
        } else {
            LaunchedEffect(messageRes) { onErrorShown() }
        }
    }

    PurpuraScreen(
        title = stringResource(R.string.title_detail),
        modifier = modifier,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            // Editar solo tiene sentido sobre un evento ya cargado; se bloquea mientras hay trabajo.
            if (state.event != null) {
                IconButton(onClick = onEdit, enabled = !state.isWorking) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.detail_edit),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Crossfade suaviza el salto cargando -> contenido / error.
            Crossfade(targetState = detailPhase(state), label = "detail-phase") { phase ->
                when (phase) {
                    DetailPhase.Content -> state.event?.let { event ->
                        DetailContent(
                            event = event,
                            working = state.isWorking,
                            onChangeStatus = onChangeStatus,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onOpenMap = onOpenMap,
                        )
                    }
                    DetailPhase.Loading -> LoadingState()
                    DetailPhase.Error -> ErrorState(
                        icon = Icons.Outlined.ErrorOutline,
                        message = stringResource(R.string.detail_load_error_title),
                        retryLabel = stringResource(R.string.detail_retry),
                        onRetry = onRetry,
                    )
                }
            }

            // Indicador de trabajo en vuelo (cambio de estatus / borrado) sin tapar el contenido.
            if (state.isWorking) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = Spacing.sm),
                )
            }
        }
    }
}

// Fase visible derivada del estado: prioriza el evento cargado para no parpadear durante el trabajo.
private enum class DetailPhase { Content, Loading, Error }

private fun detailPhase(state: DetailUiState): DetailPhase = when {
    state.event != null -> DetailPhase.Content
    state.loadFailed -> DetailPhase.Error
    else -> DetailPhase.Loading
}

@Composable
private fun DetailContent(
    event: Event,
    working: Boolean,
    onChangeStatus: (EventStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenMap: (() -> Unit)?,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screenH)
            .padding(bottom = Spacing.xl),
    ) {
        DetailHeader(event)

        Spacer(Modifier.height(Spacing.section))
        DetailInfo(event, onOpenMap)

        Spacer(Modifier.height(Spacing.section))
        StatusSection(current = event.status, working = working, onChangeStatus = onChangeStatus)

        Spacer(Modifier.height(Spacing.section))
        DetailActions(
            working = working,
            onEdit = onEdit,
            onDeleteRequest = { showDeleteDialog = true },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.detail_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.detail_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun DetailHeader(event: Event) {
    Column {
        Text(
            text = EventDisplay.formatFullDate(event.startsAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxs))
        // Hora destacada: numero grande en primary (jerarquia del spec §4).
        Text(
            text = EventDisplay.formatTime(event.startsAt),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            EventTypeBadge(event.type)
            EventStatusBadge(event.status)
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = event.description,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DetailInfo(event: Event, onOpenMap: (() -> Unit)?) {
    val locationLabel = event.location.label?.takeIf { it.isNotBlank() }
    val hasCoordinates = event.location.lat != 0.0 || event.location.lng != 0.0

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.item)) {
        InfoRow(
            icon = Icons.Outlined.Person,
            label = stringResource(R.string.detail_contact_label),
            value = event.contact.name,
            modifier = Modifier.fillMaxWidth(),
        )
        InfoRow(
            icon = Icons.Outlined.Place,
            label = stringResource(R.string.detail_location_label),
            value = locationLabel
                ?: if (hasCoordinates) stringResource(R.string.event_location_on_map)
                else stringResource(R.string.detail_no_location),
            modifier = Modifier.fillMaxWidth(),
        )
        // El mapa pulido reemplaza el look crudo: solo cuando hay coordenadas reales. La affordance
        // "Abrir en Maps" (spec §7) aparece solo si el Fragment cableo onOpenMap.
        if (hasCoordinates) {
            MapCard(
                lat = event.location.lat,
                lng = event.location.lng,
                label = locationLabel,
                onOpenExternal = onOpenMap,
            )
        }
        InfoRow(
            icon = Icons.Outlined.Notifications,
            label = stringResource(R.string.detail_reminder_label),
            value = stringResource(EventDisplay.reminderLabel(event.reminder)),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StatusSection(
    current: EventStatus,
    working: Boolean,
    onChangeStatus: (EventStatus) -> Unit,
) {
    // labelOf de SegmentedToggle no es @Composable: resolvemos las etiquetas antes (stringResource
    // es valido aqui, dentro del cuerpo composable).
    val labels = EventStatus.entries.associateWith { stringResource(EventDisplay.statusLabel(it)) }
    SectionHeader(stringResource(R.string.detail_status_label))
    SegmentedToggle(
        options = EventStatus.entries,
        selected = current,
        // Evita disparos durante un cambio en vuelo o re-seleccionar el mismo estatus.
        onSelect = { if (!working && it != current) onChangeStatus(it) },
        labelOf = { labels.getValue(it) },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DetailActions(
    working: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Button(
        onClick = onEdit,
        enabled = !working,
        shape = Pill,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.Edit, contentDescription = null)
        Spacer(Modifier.size(Spacing.sm))
        Text(stringResource(R.string.detail_edit))
    }
    Spacer(Modifier.height(Spacing.sm))
    // Eliminar en estilo de peligro: TextButton en color error, jerarquia secundaria.
    TextButton(
        onClick = onDeleteRequest,
        enabled = !working,
        shape = Pill,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
        Spacer(Modifier.size(Spacing.sm))
        Text(stringResource(R.string.detail_delete))
    }
}
