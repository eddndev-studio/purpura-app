package com.eddndev.purpura.ui.heatmap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint

// Mapa de calor mensual (densidad de eventos por dia). Migrado a Compose: el Fragment solo monta la
// pantalla; el estado vive en HeatmapViewModel y la UI en HeatmapScreen. Tocar un dia muestra su
// conteo en un Snackbar (gestionado dentro del composable). No usa permisos ni navegacion propia.
@AndroidEntryPoint
class HeatmapFragment : Fragment() {

    private val viewModel: HeatmapViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        HeatmapScreen(
            state = state,
            onPrevMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onErrorShown = viewModel::errorShown,
        )
    }
}
