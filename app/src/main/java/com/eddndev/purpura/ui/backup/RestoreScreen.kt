package com.eddndev.purpura.ui.backup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.ui.theme.Pill

/**
 * Restaurar (REQ-BACKUP-002) en Compose. Misma estructura visual que Respaldo: icono, titulo/cuerpo,
 * boton primario "Restaurar desde Drive", boton outline "Restaurar desde un archivo" y un spinner que
 * aparece mientras se trabaja. El resumen del import [ImportResult] y los errores son avisos de un
 * solo uso (snackbar) que se limpian con [onResultShown]/[onErrorShown]. La logica de datos vive en
 * [RestoreViewModel]; esta pantalla solo recibe estado y callbacks. La autorizacion de Drive, la
 * apertura del archivo y el dialogo de seleccion de respaldos los resuelve el Fragment.
 */
@Composable
fun RestoreScreen(
    state: RestoreUiState,
    onRestoreFromDrive: () -> Unit,
    onRestoreFromFile: () -> Unit,
    onResultShown: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resumen del import: aviso de un solo uso (se construye con el formato de restore_result_summary).
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

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cloud_download),
                contentDescription = stringResource(R.string.restore_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.restore_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.restore_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRestoreFromDrive,
                enabled = !state.isWorking,
                shape = Pill,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.restore_action_drive))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRestoreFromFile,
                enabled = !state.isWorking,
                shape = Pill,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_download),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.restore_action_file))
            }
            Spacer(Modifier.height(16.dp))
            // El spinner aparece/desaparece con una transicion suave mientras se restaura.
            AnimatedVisibility(visible = state.isWorking) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
