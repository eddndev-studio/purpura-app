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
import com.eddndev.purpura.databinding.FragmentBackupBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

// Respaldo (REQ-BACKUP-001). El Fragment es el unico que toca el Storage Access Framework: abre
// CreateDocument con el nombre sugerido por el VM, y al volver con el Uri le pasa al VM un abridor
// de OutputStream. Asi el destino real (Drive, Dropbox, local) lo elige el selector del sistema.
@AndroidEntryPoint
class BackupFragment : Fragment() {

    private var _binding: FragmentBackupBinding? = null
    private val binding get() = _binding!!
    private val viewModel: BackupViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentBackupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backupButton.setOnClickListener { viewModel.prepareBackup() }
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: BackupUiState) {
        binding.backupButton.isEnabled = !state.isWorking
        binding.backupProgress.isVisible = state.isWorking

        state.pendingFileName?.let { fileName ->
            createDocument.launch(fileName)
            viewModel.launchHandled()
        }
        state.savedCount?.let { count ->
            val message = resources.getQuantityString(R.plurals.backup_result, count, count)
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.messageShown()
        }
        state.infoRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.messageShown()
        }
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.messageShown()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val MIME_JSON = "application/json"
    }
}
