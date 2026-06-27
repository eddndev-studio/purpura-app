package com.eddndev.purpura.ui.backup

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDownload
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.ui.compose.HeroActionScreen
import com.eddndev.purpura.ui.compose.InfoCard
import com.eddndev.purpura.ui.compose.LoadingButton
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Restaurar (REQ-BACKUP-002) en Compose. Espeja a Respaldo: reusa el shell [HeroActionScreen] (hero +
 * titulo + cuerpo + extra + acciones) para compartir ritmo al pixel. Dos acciones jerarquizadas a
 * ancho completo: "Restaurar desde Google Drive" (primaria, con progreso en linea via [LoadingButton])
 * y "Restaurar desde un archivo" (secundaria, outline, solo deshabilitada al trabajar).
 *
 * El resumen del import [ImportResult] se conserva como confirmacion EN LINEA (tarjeta en el slot
 * extra) en vez de un snackbar fugaz: el ViewModel ya lo limpia al iniciar una nueva restauracion, asi
 * que persiste hasta entonces. Solo el error es un aviso de un solo uso (snackbar, [onErrorShown]).
 * [onResultShown] se mantiene en la firma por compatibilidad con el Fragment/tests aunque la
 * confirmacion ya no se autodescarta.
 */
@Composable
fun RestoreScreen(
    state: RestoreUiState,
    onRestoreFromDrive: () -> Unit,
    onRestoreFromFile: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onResultShown: () -> Unit,
    onErrorShown: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // Error: aviso de un solo uso (transitorio).
    state.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    val working = state.isWorking
    val progressLabel = stringResource(R.string.restore_in_progress)

    HeroActionScreen(
        screenTitle = stringResource(R.string.title_restore),
        onBack = onBack,
        heroIcon = Icons.Outlined.CloudDownload,
        title = stringResource(R.string.restore_title),
        body = stringResource(R.string.restore_body),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        extra = {
            // Confirmacion persistente: el desglose del import (nuevos/actualizados/omitidos/error) en
            // una tarjeta, no en un snackbar que desaparece solo. Crossfade evita el salto al aparecer.
            Crossfade(
                targetState = state.result,
                animationSpec = tween(220),
                label = "restoreResult",
            ) { result ->
                if (result != null) {
                    ResultCard(result = result, modifier = Modifier.padding(top = Spacing.xl))
                }
            }
        },
        actions = {
            LoadingButton(
                onClick = onRestoreFromDrive,
                text = stringResource(R.string.restore_action_drive),
                isLoading = working,
                leadingIcon = Icons.Outlined.CloudDownload,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        if (working) stateDescription = progressLabel
                    },
            )
            Spacer(Modifier.height(Spacing.md))
            OutlinedButton(
                onClick = onRestoreFromFile,
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
                Text(stringResource(R.string.restore_action_file))
            }
        },
    )
}

// Tarjeta de resultado del import: cuatro conteos con la cifra en titleLarge primario. Reusa la
// InfoCard de la fundacion (surfaceContainerLow + sombra suave de marca) para leerse igual que las
// tarjetas de datos del Detalle y Cuenta, no como una superficie ajena.
@Composable
private fun ResultCard(result: ImportResult, modifier: Modifier = Modifier) {
    InfoCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.restore_result_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        ResultRow(stringResource(R.string.restore_count_new), result.imported)
        ResultRow(stringResource(R.string.restore_count_updated), result.updated)
        ResultRow(stringResource(R.string.restore_count_skipped), result.skipped)
        ResultRow(stringResource(R.string.restore_count_failed), result.failed)
    }
}

@Composable
private fun ResultRow(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
