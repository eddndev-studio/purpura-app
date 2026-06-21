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
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.common.DriveAuth
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

// Respaldo (REQ-BACKUP-001). Migrada a Compose (BackupScreen): el Fragment monta la pantalla y conserva
// la plomeria que no puede vivir en el composable. Dos caminos:
//  - Google Drive (API): el Fragment asegura la autorizacion de Google (scope drive.file) y luego el
//    VM sube el respaldo via la API de Drive.
//  - Archivo: el Fragment abre CreateDocument (Storage Access Framework) y le pasa al VM un abridor de
//    OutputStream; el destino real (Drive/Dropbox/local) lo elige el selector del sistema.
// El composable recibe estado + callbacks (onBackupToDrive / onBackupToFile / messageShown); el
// Fragment observa solo pendingFileName para abrir el selector (no se puede hacer desde Compose).
@AndroidEntryPoint
class BackupFragment : Fragment() {

    private val viewModel: BackupViewModel by viewModels()

    // Accion a ejecutar tras conceder el permiso de Drive (respaldar). Null fuera del flujo de auth.
    private var pendingDriveAction: (() -> Unit)? = null

    private val driveAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        val action = pendingDriveAction
        pendingDriveAction = null
        if (action != null && DriveAuth.isAuthorized(requireContext())) {
            action()
        } else {
            // Auth rechazada: no esta en el UiState, asi que el aviso vive en el Fragment (sobre la
            // ComposeView, ya que no hay ViewBinding).
            Snackbar.make(requireView(), R.string.backup_drive_auth_needed, Snackbar.LENGTH_LONG).show()
        }
    }

    private val createDocument = registerForActivityResult(
        ActivityResultContracts.CreateDocument(MIME_JSON),
    ) { uri ->
        if (uri == null) {
            viewModel.backupCancelled()
        } else {
            val resolver = requireContext().contentResolver
            viewModel.saveBackup { resolver.openOutputStream(uri) ?: throw IOException("destino no disponible") }
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
        BackupScreen(
            state = state,
            onBack = { findNavController().navigateUp() },
            onBackupToDrive = { ensureDriveAuthThen { viewModel.backupToDrive() } },
            onBackupToFile = viewModel::prepareBackup,
            onMessageShown = viewModel::messageShown,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observePendingFileName()
    }

    // El selector del sistema (CreateDocument) no se puede lanzar desde Compose: el Fragment observa
    // pendingFileName y abre el selector, confirmando el lanzamiento para no relanzarlo.
    private fun observePendingFileName() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    state.pendingFileName?.let { fileName ->
                        createDocument.launch(fileName)
                        viewModel.launchHandled()
                    }
                }
            }
        }
    }

    private companion object {
        const val MIME_JSON = "application/json"
    }
}
