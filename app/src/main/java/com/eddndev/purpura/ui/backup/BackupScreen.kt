package com.eddndev.purpura.ui.backup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.InsertDriveFile
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.BrandIconBadge
import com.eddndev.purpura.ui.compose.HeroActionScreen
import com.eddndev.purpura.ui.compose.LoadingButton
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Respaldo (REQ-BACKUP-001) en Compose. Reusa el shell [HeroActionScreen] (hero + titulo + cuerpo +
 * extra + acciones) para compartir ritmo al pixel con Restaurar y evitar re-implementar el centrado.
 * Dos acciones jerarquizadas full-width: "Respaldar en Google Drive" (primaria con progreso en linea)
 * y "Guardar en un archivo" (OutlinedButton, secundaria). La autorizacion de Drive y el selector de
 * archivo (SAF) los resuelve el Fragment; aqui solo se disparan [onBackupToDrive] y [onBackupToFile].
 *
 * Avisos transitorios (infoRes / errorRes) van por snackbar y se confirman con [onMessageShown]. El
 * EXITO no usa snackbar: se conserva como confirmacion en linea (savedCount, en el slot extra) para
 * que el usuario lo vea sin que desaparezca solo.
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

    // Aviso informativo (respaldo vacio): transitorio, via snackbar.
    state.infoRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }
    // Error (E/S al guardar, Drive sin autorizar, etc.): transitorio, via snackbar.
    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    val working = state.isWorking
    val progressLabel = stringResource(R.string.backup_in_progress)

    // Titulo del app bar: reusa account_backup ("Respaldo") por rule (no inventar strings).
    HeroActionScreen(
        screenTitle = stringResource(R.string.account_backup),
        onBack = onBack,
        heroIcon = Icons.Outlined.CloudUpload,
        title = stringResource(R.string.backup_title),
        body = stringResource(R.string.backup_body),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        extra = {
            // Confirmacion persistente bajo el cuerpo: en un resultado retenido (savedCount) mostramos
            // un hero en tono de exito + conteo. Crossfade evita el "salto" al aparecer/desaparecer.
            Crossfade(
                targetState = state.savedCount,
                animationSpec = tween(220),
                label = "backupResult",
            ) { count ->
                if (count != null) {
                    BackupSuccess(count = count, modifier = Modifier.padding(top = Spacing.xl))
                }
            }
        },
        actions = {
            // Accion primaria con progreso EN LINEA (sin spinner aparte que mueva el layout). El
            // liveRegion anuncia el trabajo en curso a lectores de pantalla sin tapar la etiqueta.
            LoadingButton(
                onClick = onBackupToDrive,
                text = stringResource(R.string.backup_action_drive),
                isLoading = working,
                leadingIcon = Icons.Outlined.CloudUpload,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        if (working) stateDescription = progressLabel
                    },
            )
            Spacer(Modifier.height(Spacing.md))
            // Secundaria: solo deshabilitada mientras se trabaja (sin spinner propio).
            OutlinedButton(
                onClick = onBackupToFile,
                enabled = !working,
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
        },
    )
}

// Confirmacion en linea: hero en tono de exito (tertiaryContainer, dentro de la marca mono-purpura,
// sin color dinamico) + conteo en labelLarge primario.
@Composable
private fun BackupSuccess(count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandIconBadge(
            icon = Icons.Outlined.CloudDone,
            size = 56.dp,
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = pluralStringResource(R.plurals.backup_result, count, count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}
