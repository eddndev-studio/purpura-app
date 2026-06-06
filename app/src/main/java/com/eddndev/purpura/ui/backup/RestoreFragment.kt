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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.IOException

// Restaurar (REQ-BACKUP-002). El Fragment abre OpenDocument (filtrado a JSON) y al volver con el
// Uri le pasa al VM un abridor de InputStream. El selector del sistema lista los archivos de Drive,
// Dropbox o locales.
@AndroidEntryPoint
class RestoreFragment : Fragment() {

    private var _binding: FragmentRestoreBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RestoreViewModel by viewModels()

    private val openDocument = registerForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // uri null = el usuario cancelo el selector: no hay nada que restaurar.
        if (uri != null) {
            val resolver = requireContext().contentResolver
            viewModel.restoreFrom { resolver.openInputStream(uri) ?: throw IOException("fuente no disponible") }
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
        binding.restoreButton.setOnClickListener { openDocument.launch(arrayOf(MIME_JSON)) }
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
        binding.restoreButton.isEnabled = !state.isWorking
        binding.restoreProgress.isVisible = state.isWorking

        state.result?.let { result ->
            Snackbar.make(binding.root, summaryOf(result), Snackbar.LENGTH_LONG).show()
            viewModel.resultShown()
        }
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
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
