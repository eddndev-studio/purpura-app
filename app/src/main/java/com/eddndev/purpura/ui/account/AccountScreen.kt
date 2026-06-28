package com.eddndev.purpura.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.ui.compose.PurpuraListRow
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

/**
 * Cuenta (hub): cabecera con la sesion (avatar + nombre/email), seccion Datos (Respaldo/Restaurar),
 * seccion Aplicacion (Acerca de) y cierre de sesion con confirmacion. Reemplaza al antiguo drawer:
 * agrupa todo lo secundario detras de una sola pestana del bottom nav. El ritmo vertical usa Spacers
 * explicitos (no spacedBy) para separar exactamente las secciones (24dp) de las filas (8dp).
 */
@Composable
fun AccountScreen(
    session: Session?,
    versionName: String,
    uiState: AccountUiState,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onAbout: () -> Unit,
    onLinkGoogle: () -> Unit,
    onUnlinkGoogle: () -> Unit,
    onLogout: () -> Unit,
    onDeleteAccount: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }
    val errorColor = MaterialTheme.colorScheme.error
    val snackbarHostState = remember { SnackbarHostState() }
    val deleting = uiState.isDeletingAccount
    val updatingGoogle = uiState.isUpdatingGoogleLink
    // Mientras una accion con progreso propio (borrado o vinculacion) esta en vuelo, se deshabilita
    // TODO lo demas: las dos no deben solaparse y el resto no debe dispararse encima.
    val busy = deleting || updatingGoogle
    val deletingDesc = stringResource(R.string.account_deleting)

    // Aviso de un solo uso si el borrado de cuenta falla (la sesion se conserva intacta).
    uiState.errorRes?.let { messageRes ->
        val message = stringResource(messageRes)
        LaunchedEffect(messageRes, message) {
            snackbarHostState.showSnackbar(message)
            onErrorShown()
        }
    }

    PurpuraScreen(
        title = stringResource(R.string.title_account),
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH)
                .padding(bottom = Spacing.xl),
        ) {
            AccountHeader(session)

            // Spacing.section separa cada bloque de su SectionHeader; las filas internas van a sm.
            Spacer(Modifier.height(Spacing.section))
            SectionHeader(stringResource(R.string.account_section_data))
            PurpuraListRow(
                icon = Icons.Outlined.CloudUpload,
                title = stringResource(R.string.account_backup),
                subtitle = stringResource(R.string.account_backup_desc),
                onClick = onBackup,
            )
            Spacer(Modifier.height(Spacing.sm))
            PurpuraListRow(
                icon = Icons.Outlined.CloudDownload,
                title = stringResource(R.string.account_restore),
                subtitle = stringResource(R.string.account_restore_desc),
                onClick = onRestore,
            )

            Spacer(Modifier.height(Spacing.section))
            SectionHeader(stringResource(R.string.account_section_app))
            PurpuraListRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.account_about),
                subtitle = stringResource(R.string.account_about_desc),
                onClick = onAbout,
            )

            // Inicio de sesion: vincular/desvincular Google. Solo aparece con sesion cargada (la
            // cabecera ya la asume). googleLinked y authProvider salen del usuario en sesion.
            session?.user?.let { user ->
                Spacer(Modifier.height(Spacing.section))
                SectionHeader(stringResource(R.string.account_section_login))
                AccountGoogleRow(
                    googleLinked = user.googleLinked,
                    // Solo una cuenta con contrasena (origen formulario) puede desvincular: una de
                    // origen Google quedaria sin credencial. El backend lo reimpone con 409.
                    canUnlink = user.authProvider == AuthProvider.password,
                    updating = updatingGoogle,
                    enabled = !busy,
                    onLink = onLinkGoogle,
                    onUnlink = { showUnlinkDialog = true },
                )
            }

            // Divisor con holgura simetrica: separa la zona destructiva del resto sin pegarse al boton.
            Spacer(Modifier.height(Spacing.section))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.section))
            OutlinedButton(
                onClick = { showLogoutDialog = true },
                enabled = !busy,
                shape = Pill,
                // Rol de error: el cierre de sesion es destructivo, se tine de error sin perder la pill.
                colors = ButtonDefaults.outlinedButtonColors(contentColor = errorColor),
                border = BorderStroke(1.dp, errorColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.account_logout))
            }

            // Eliminar cuenta: la accion mas severa (irreversible, borra TODO en el servidor). Va como
            // boton de texto en rol de error bajo el de cerrar sesion: presente pero subordinado, con
            // una confirmacion reforzada. Mientras el borrado esta en vuelo muestra progreso en linea.
            Spacer(Modifier.height(Spacing.sm))
            TextButton(
                onClick = { showDeleteDialog = true },
                enabled = !busy,
                colors = ButtonDefaults.textButtonColors(contentColor = errorColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (deleting) {
                    // Anuncia el progreso a lectores de pantalla: el spinner reemplaza al texto, asi
                    // que sin esto TalkBack no diria nada del borrado en curso.
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = errorColor,
                        modifier = Modifier
                            .size(18.dp)
                            .semantics {
                                contentDescription = deletingDesc
                                liveRegion = LiveRegionMode.Polite
                            },
                    )
                } else {
                    Icon(Icons.Outlined.DeleteForever, contentDescription = null)
                    Spacer(Modifier.size(Spacing.sm))
                    Text(stringResource(R.string.account_delete_account))
                }
            }

            // Version como pie centrado: dato secundario, fuera del flujo de acciones.
            Spacer(Modifier.height(Spacing.section))
            Text(
                text = stringResource(R.string.account_version, versionName),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
            title = { Text(stringResource(R.string.about_logout_title)) },
            text = { Text(stringResource(R.string.about_logout_message)) },
            confirmButton = {
                // El confirmar tambien va con rol de error: coincide con el boton de la pantalla.
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor),
                ) { Text(stringResource(R.string.about_logout_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.about_logout_cancel))
                }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Outlined.DeleteForever, contentDescription = null, tint = errorColor) },
            title = { Text(stringResource(R.string.account_delete_title)) },
            text = { Text(stringResource(R.string.account_delete_message)) },
            confirmButton = {
                // Confirmar en rol de error: dispara el borrado irreversible (el VM gestiona el progreso).
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = errorColor),
                ) { Text(stringResource(R.string.account_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.account_delete_cancel))
                }
            },
        )
    }

    if (showUnlinkDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkDialog = false },
            icon = { Icon(Icons.Outlined.LinkOff, contentDescription = null) },
            title = { Text(stringResource(R.string.account_unlink_title)) },
            text = { Text(stringResource(R.string.account_unlink_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlinkDialog = false
                        onUnlinkGoogle()
                    },
                ) { Text(stringResource(R.string.account_unlink_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkDialog = false }) {
                    Text(stringResource(R.string.account_unlink_cancel))
                }
            },
        )
    }
}
