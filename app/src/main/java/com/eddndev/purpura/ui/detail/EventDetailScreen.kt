package com.eddndev.purpura.ui.detail

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.EventStatusBadge
import com.eddndev.purpura.ui.compose.EventTypeBadge
import com.eddndev.purpura.ui.compose.ErrorState
import com.eddndev.purpura.ui.compose.LiteLocationMap
import com.eddndev.purpura.ui.compose.LoadingState
import com.eddndev.purpura.ui.theme.Pill

/**
 * Detalle y edicion de un evento (REQ-QUERY-007..013) en Compose. Carga por id (resuelta en el
 * Fragment con load); muestra los campos, permite cambiar el estatus (chips) y eliminar (con
 * confirmacion). El mapa lite aparece solo si el evento trae coordenadas reales. La navegacion
 * (editar, regreso tras borrar) y la recarga tras editar viven en el Fragment via callbacks.
 */
@Composable
fun EventDetailScreen(
    state: DetailUiState,
    onRetry: () -> Unit,
    onChangeStatus: (EventStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDeleted: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.event != null -> DetailContent(
                    event = state.event,
                    working = state.isWorking,
                    onChangeStatus = onChangeStatus,
                    onEdit = onEdit,
                    onDelete = onDelete,
                )
                state.isLoading -> LoadingState()
                state.loadFailed -> ErrorState(
                    icon = Icons.Outlined.ErrorOutline,
                    message = stringResource(R.string.detail_load_error_title),
                    retryLabel = stringResource(R.string.detail_retry),
                    onRetry = onRetry,
                )
            }

            AnimatedVisibility(
                visible = state.isWorking,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    event: Event,
    working: Boolean,
    onChangeStatus: (EventStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text(
            text = EventDisplay.formatFullDate(event.startsAt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = EventDisplay.formatTime(event.startsAt),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventTypeBadge(event.type)
            EventStatusBadge(event.status)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = event.description,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))

        Field(label = stringResource(R.string.detail_contact_label), value = event.contact.name)
        Spacer(Modifier.height(12.dp))
        Field(
            label = stringResource(R.string.detail_location_label),
            value = event.location.label?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.detail_no_location),
        )

        val hasCoordinates = event.location.lat != 0.0 || event.location.lng != 0.0
        if (hasCoordinates) {
            Spacer(Modifier.height(12.dp))
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                LiteLocationMap(
                    lat = event.location.lat,
                    lng = event.location.lng,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Field(
            label = stringResource(R.string.detail_reminder_label),
            value = stringResource(EventDisplay.reminderLabel(event.reminder)),
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.detail_status_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EventStatus.entries.forEach { status ->
                FilterChip(
                    selected = event.status == status,
                    onClick = { if (!working) onChangeStatus(status) },
                    enabled = !working,
                    label = { Text(stringResource(EventDisplay.statusLabel(status))) },
                    shape = Pill,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onEdit,
            enabled = !working,
            shape = Pill,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.detail_edit))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            enabled = !working,
            shape = Pill,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.detail_delete))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text(stringResource(R.string.detail_delete_confirm)) }
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
private fun Field(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}
