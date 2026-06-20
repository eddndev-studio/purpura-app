package com.eddndev.purpura.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.repository.CloudBackup
import com.eddndev.purpura.ui.common.DriveAuth
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

// Restaurar (REQ-BACKUP-002). Migrada a Compose (RestoreScreen): el Fragment monta la pantalla y
// conserva la plomeria. Dos caminos:
//  - Google Drive (API): el Fragment asegura la autorizacion, el VM lista los respaldos de Drive y el
//    usuario elige cual restaurar en un dialogo; el VM descarga e importa.
//  - Archivo: el Fragment abre OpenDocument (filtrado a JSON) y le pasa al VM un abridor de
//    InputStream; el selector del sistema lista archivos de Drive, Dropbox o locales.
@AndroidEntryPoint
class RestoreFragment : Fragment() {

    private val viewModel: RestoreViewModel by viewModels()

    // Accion a ejecutar tras conceder el permiso de Drive (listar respaldos). Null fuera del flujo.
    private var pendingDriveAction: (() -> Unit)? = null

    private val driveAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        val action = pendingDriveAction
        pendingDriveAction = null
        if (action != null && DriveAuth.isAuthorized(requireContext())) {
            action()
        } else {
            view?.let { Snackbar.make(it, R.string.restore_drive_auth_needed, Snackbar.LENGTH_LONG).show() }
        }
    }

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // uri null = el usuario cancelo el selector: no hay nada que restaurar.
        if (uri != null) {
            val resolver = requireContext().contentResolver
            viewModel.restoreFrom { resolver.openInputStream(uri) ?: throw IOException("fuente no disponible") }
        }
    }

    // Asegura la autorizacion de Drive antes de [action]: si ya hay permiso la ejecuta, si no lanza el
    // consentimiento de Google y la encola para correrla al volver.
    private fun ensureDriveAuthThen(action: () -> Unit) {
        if (DriveAuth.isAuthorized(requireContext())) {
            action()
        } else {
            pendingDriveAction = action
            driveAuthLauncher.launch(DriveAuth.client(requireContext()).signInIntent)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        RestoreScreen(
            state = state,
            onRestoreFromDrive = { ensureDriveAuthThen { viewModel.loadDriveBackups() } },
            onRestoreFromFile = { openDocument.launch(arrayOf(MIME_JSON)) },
            onResultShown = viewModel::resultShown,
            onErrorShown = viewModel::errorShown,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeDriveBackups()
    }

    // El dialogo de seleccion de respaldos de Drive se queda en la vista clasica (MaterialAlertDialog):
    // observa driveBackups, abre el picker (o avisa si esta vacio) y consume la lista para no reabrirlo
    // al rotar. El resto de avisos (resumen/errores) los muestra RestoreScreen via snackbar.
    private fun observeDriveBackups() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.driveBackups?.let { backups ->
                        viewModel.driveBackupsShown()
                        if (backups.isEmpty()) {
                            view?.let {
                                Snackbar.make(it, R.string.restore_drive_empty, Snackbar.LENGTH_LONG).show()
                            }
                        } else {
                            showDrivePicker(backups)
                        }
                    }
                }
            }
        }
    }

    // Dialogo para elegir cual respaldo de Drive restaurar (por nombre de archivo, mas reciente arriba).
    private fun showDrivePicker(backups: List<CloudBackup>) {
        val names = backups.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restore_drive_pick_title)
            .setItems(names) { _, which -> viewModel.restoreFromDrive(backups[which].id) }
            .setNegativeButton(R.string.detail_delete_cancel, null)
            .show()
    }

    private companion object {
        const val MIME_JSON = "application/json"
    }
}
