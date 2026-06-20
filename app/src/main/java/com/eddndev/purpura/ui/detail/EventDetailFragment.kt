package com.eddndev.purpura.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import com.eddndev.purpura.ui.common.RESULT_EVENT_EDITED
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint

// Detalle y edicion de un evento (REQ-QUERY-007..013). Migrado a Compose (EventDetailScreen): el
// Fragment resuelve el id (argumento de navegacion), recarga al volver del formulario en edicion y
// gestiona la navegacion (editar / regreso tras borrar). El estado vive en DetailViewModel.
@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    private val viewModel: DetailViewModel by viewModels()
    private val eventId: String get() = arguments?.getString(ARG_EVENT_ID).orEmpty()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        EventDetailScreen(
            state = state,
            onRetry = viewModel::retry,
            onChangeStatus = viewModel::changeStatus,
            onEdit = ::openEdit,
            onDelete = viewModel::delete,
            onDeleted = { findNavController().navigateUp() },
            onErrorShown = viewModel::errorShown,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (eventId.isBlank()) {
            findNavController().navigateUp()
            return
        }
        // El formulario en modo edicion avisa al guardar; recargamos para reflejar los cambios.
        setFragmentResultListener(RESULT_EVENT_EDITED) { _, _ -> viewModel.refresh() }
        viewModel.load(eventId)
    }

    // Abre el formulario de Anadir Evento en modo edicion (con el id), reutilizandolo. Guarda contra
    // doble navegacion si el Detalle ya no es el destino actual.
    private fun openEdit() {
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.eventDetailFragment) {
            controller.navigate(R.id.addEventFragment, bundleOf(ARG_EVENT_ID to eventId))
        }
    }
}
