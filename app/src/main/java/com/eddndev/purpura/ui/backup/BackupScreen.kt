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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.Pill

/**
 * Respaldo (REQ-BACKUP-001) en Compose. Pantalla de accion centrada: icono + titulo/cuerpo y dos
 * botones, "Respaldar en Google Drive" (primario) y "Guardar en un archivo" (outline). El spinner
 * aparece mientras [BackupUiState.isWorking]. La logica vive en [BackupViewModel]; esta pantalla solo
 * recibe estado y callbacks. La autorizacion de Drive y el selector de archivo (SAF) los resuelve el
 * Fragment; aqui solo se disparan [onBackupToDrive] y [onBackupToFile].
 *
 * Los avisos de un solo uso (savedCount / infoRes / errorRes) se muestran como snackbar via
 * [LaunchedEffect] y se confirman con [onMessageShown] (= messageShown del VM).
 */
@Composable
fun BackupScreen(
    state: BackupUiState,
    onBackupToDrive: () -> Unit,
    onBackupToFile: () -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Resultado con plural: "%d evento(s) respaldado(s)". Se resuelve en scope composable.
    state.savedCount?.let { count ->
        val message = pluralStringResource(R.plurals.backup_result, count, count)
        LaunchedEffect(count, message) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
    // Aviso informativo (p. ej. respaldo vacio).
    state.infoRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
    // Error (E/S al guardar, Drive sin autorizar, etc.).
    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
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
                painter = painterResource(R.drawable.ic_cloud_upload),
                contentDescription = stringResource(R.string.backup_title),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.backup_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onBackupToDrive,
                enabled = !state.isWorking,
                shape = Pill,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.backup_action_drive))
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onBackupToFile,
                enabled = !state.isWorking,
                shape = Pill,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.backup_action_file))
            }
            // El spinner aparece/desaparece con animacion mientras se trabaja.
            AnimatedVisibility(visible = state.isWorking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
