package com.eddndev.purpura.ui.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Restaurar (REQ-BACKUP-002) en Compose. Espeja a Respaldo: hero centrado (icono en disco + titulo y
 * cuerpo) y dos acciones jerarquizadas a ancho completo: "Restaurar desde Google Drive" (primario) y
 * "Restaurar desde un archivo" (secundario, outline). El spinner aparece mientras se trabaja.
 *
 * El resumen del import [ImportResult] y los errores son avisos de un solo uso (snackbar) que se
 * limpian con [onResultShown]/[onErrorShown]. La logica vive en [RestoreViewModel]; esta pantalla solo
 * recibe estado y callbacks. La autorizacion de Drive, la apertura del archivo y el dialogo de
 * seleccion de respaldos los resuelve el Fragment. [onBack] navega hacia atras (pantalla con flecha).
 */
@Composable
fun RestoreScreen(
    state: RestoreUiState,
    onRestoreFromDrive: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onResultShown: () -> Unit,
    onErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resumen del import: aviso de un solo uso (formato restore_result_summary).
    state.result?.let { result ->
        val message = summaryOf(result)
        LaunchedEffect(result, message) {
            snackbarHostState.showSnackbar(message)
            onResultShown()
        }
    }

    // Error: aviso de un solo uso.
    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    PurpuraScreen(
        title = stringResource(R.string.title_restore),
        modifier = modifier,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Hero: icono dentro de un disco de marca para alejar el "look de demo".
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.restore_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.restore_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xxl))
            // Accion primaria: restaurar desde Drive.
            Button(
                onClick = onRestoreFromDrive,
                enabled = !state.isWorking,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.restore_action_drive))
            }
            Spacer(Modifier.height(Spacing.md))
            // Accion secundaria: restaurar desde un archivo local/.json.
            OutlinedButton(
                onClick = onRestoreFromFile,
                enabled = !state.isWorking,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.restore_action_file))
            }

            // El spinner aparece/desaparece con una transicion suave mientras se restaura.
            AnimatedVisibility(visible = state.isWorking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(Spacing.xl))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// Resumen del import en el formato de restore_result_summary (nuevos/actualizados/omitidos/error).
@Composable
private fun summaryOf(result: ImportResult): String = stringResource(
    R.string.restore_result_summary,
    result.imported,
    result.updated,
    result.skipped,
    result.failed,
)
