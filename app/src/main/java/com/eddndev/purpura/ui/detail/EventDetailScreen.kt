package com.eddndev.purpura.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.ui.compose.DetailSkeleton
import com.eddndev.purpura.ui.compose.ErrorState
import com.eddndev.purpura.ui.compose.PurpuraScreen

/**
 * Detalle de un evento (REQ-QUERY-007..013) en Compose, sobre [PurpuraScreen] con flecha atras. La
 * cabecera lidera con la identidad (descripcion como titular, fecha y hora); las filas de info viven
 * agrupadas en una InfoCard bajo la seccion "Detalles", con un [com.eddndev.purpura.ui.compose.MapCard]
 * hero cuando hay coordenadas. El estatus se cambia con un SegmentedToggle. Las acciones primarias
 * (Editar / Eliminar) se anclan al pie via el slot bottomBar, no flotan en la cabecera.
 *
 * Trabajo en vuelo (cambio de estatus / borrado): una barra de progreso lineal a ras del app bar, en
 * vez de un spinner flotante; las acciones afectadas quedan bloqueadas mientras tanto. La carga inicial
 * usa el esqueleto del Detalle (no un spinner centrado); el fallo de carga usa [ErrorState] con
 * reintentar y el error sobre un evento ya visible es un aviso de un solo uso (snackbar).
 *
 * [onOpenMap] dispara la affordance "Abrir en Maps" del MapCard (spec §7); la pantalla es pura, asi que
 * el Intent lo resuelve el Fragment.
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
    var showDeleteDialog by remember { mutableStateOf(false) }

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
        // Acciones primarias ancladas al pie solo cuando hay un evento cargado.
        bottomBar = state.event?.let {
            {
                DetailBottomBar(
                    working = state.isWorking,
                    onEdit = onEdit,
                    onDeleteRequest = { showDeleteDialog = true },
                )
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
                            onOpenMap = onOpenMap,
                        )
                    }
                    DetailPhase.Loading -> DetailSkeleton()
                    DetailPhase.Error -> ErrorState(
                        icon = Icons.Outlined.ErrorOutline,
                        message = stringResource(R.string.detail_load_error_title),
                        retryLabel = stringResource(R.string.detail_retry),
                        onRetry = onRetry,
                    )
                }
            }

            // Trabajo en vuelo (cambio de estatus / borrado): barra lineal a ras del app bar, sin tapar
            // el contenido. liveRegion Polite para que el lector de pantalla anuncie el procesamiento.
            if (state.isWorking) {
                val workingDesc = stringResource(R.string.detail_working)
                LinearProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = workingDesc
                        },
                )
            }
        }
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = { showDeleteDialog = false },
        )
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
private fun DeleteConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
        text = { Text(stringResource(R.string.detail_delete_confirm_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) { Text(stringResource(R.string.detail_delete_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_delete_cancel))
            }
        },
    )
}
