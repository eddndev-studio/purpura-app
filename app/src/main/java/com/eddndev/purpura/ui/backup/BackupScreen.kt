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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Respaldo (REQ-BACKUP-001) en Compose. Pantalla de accion centrada bajo [PurpuraScreen] (app bar con
 * flecha atras): icono en pastilla, titulo/cuerpo y dos acciones jerarquizadas full-width: "Respaldar
 * en Google Drive" (Button primario pill) y "Guardar en un archivo" (OutlinedButton pill, secundaria).
 * El spinner aparece mientras [BackupUiState.isWorking]. La logica vive en [BackupViewModel]; esta
 * pantalla solo recibe estado y callbacks. La autorizacion de Drive y el selector de archivo (SAF) los
 * resuelve el Fragment; aqui solo se disparan [onBackupToDrive] y [onBackupToFile].
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
    onBack: () -> Unit,
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

    // Titulo del app bar: reusa account_backup ("Respaldo") por rule 3 (no inventar strings).
    PurpuraScreen(
        title = stringResource(R.string.account_backup),
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
            // Hero: icono dentro de un disco de marca (espeja a Restaurar) para alejar el "look de demo".
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = stringResource(R.string.backup_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.backup_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xxl))
            // Accion primaria: respaldar en Drive (full width, pill).
            Button(
                onClick = onBackupToDrive,
                enabled = !state.isWorking,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cloud_upload),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.backup_action_drive))
            }
            Spacer(Modifier.height(Spacing.md))
            // Accion secundaria: guardar en archivo (outline, full width, pill).
            OutlinedButton(
                onClick = onBackupToFile,
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
                Text(stringResource(R.string.backup_action_file))
            }
            // El spinner aparece/desaparece con una transicion suave mientras se trabaja.
            AnimatedVisibility(visible = state.isWorking) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(Spacing.xl))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
