package com.eddndev.purpura.ui.backup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentRestoreBinding
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.repository.CloudBackup
import com.eddndev.purpura.ui.common.DriveAuth
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

// Restaurar (REQ-BACKUP-002). Dos caminos:
//  - Google Drive (API): el Fragment asegura la autorizacion, el VM lista los respaldos de Drive y el
//    usuario elige cual restaurar en un dialogo; el VM descarga e importa.
//  - Archivo: el Fragment abre OpenDocument (filtrado a JSON) y le pasa al VM un abridor de
//    InputStream; el selector del sistema lista archivos de Drive, Dropbox o locales.
@AndroidEntryPoint
class RestoreFragment : Fragment() {

    private var _binding: FragmentRestoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestoreViewModel by viewModels()

    private var pendingDriveAction: (() -> Unit)? = null

    private val driveAuthLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        val action = pendingDriveAction
        pendingDriveAction = null
        if (action != null && DriveAuth.isAuthorized(requireContext())) {
            action()
        } else {
            Snackbar.make(binding.root, R.string.restore_drive_auth_needed, Snackbar.LENGTH_LONG).show()
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
    ): View {
        _binding = FragmentRestoreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.restoreDriveButton.setOnClickListener { ensureDriveAuthThen { viewModel.loadDriveBackups() } }
        binding.restoreFileButton.setOnClickListener { openDocument.launch(arrayOf(MIME_JSON)) }
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: RestoreUiState) {
        binding.restoreDriveButton.isEnabled = !state.isWorking
        binding.restoreFileButton.isEnabled = !state.isWorking
        binding.restoreProgress.isVisible = state.isWorking

        // Lista de respaldos de Drive lista: abre el dialogo de seleccion (o avisa si esta vacia) y la
        // consume para no reabrirlo al rotar.
        state.driveBackups?.let { backups ->
            viewModel.driveBackupsShown()
            if (backups.isEmpty()) {
                Snackbar.make(binding.root, R.string.restore_drive_empty, Snackbar.LENGTH_LONG).show()
            } else {
                showDrivePicker(backups)
            }
        }
        state.result?.let { result ->
            Snackbar.make(binding.root, summaryOf(result), Snackbar.LENGTH_LONG).show()
            viewModel.resultShown()
        }
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
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

    private fun summaryOf(result: ImportResult): String = getString(
        R.string.restore_result_summary,
        result.imported,
        result.updated,
        result.skipped,
        result.failed,
    )

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MIME_JSON = "application/json"
    }
}
