package com.eddndev.purpura.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint

// Pantalla principal tras autenticar (REQ-HOME-001/002). Migrada a Compose: el Fragment solo monta
// la pantalla y resuelve navegacion y el menu de heatmap; el estado vive en HomeViewModel y la UI en
// HomeScreen. Lista los eventos de hoy + proximos 4 dias con pull-to-refresh; el FAB crea un evento.
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        HomeScreen(
            state = state,
            onRefresh = viewModel::refresh,
            onAddEvent = { findNavController().navigate(R.id.addEventFragment) },
            onEventClick = ::onEventClick,
            onErrorShown = viewModel::errorShown,
        )
    }

    // Navega al detalle del evento (guardado contra doble navegacion).
    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.homeFragment)
    }
}
