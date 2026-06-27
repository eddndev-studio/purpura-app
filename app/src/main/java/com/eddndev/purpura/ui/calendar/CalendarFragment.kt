package com.eddndev.purpura.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate

// Calendario mensual (REQ-CAL-001..003). Migrado a Compose: el Fragment solo monta la pantalla y
// resuelve la navegacion al Detalle y al Mapa de calor (toggle Mes/Calor); el estado vive en
// CalendarViewModel y la UI en CalendarScreen. Pinta la rejilla del mes con puntos por tipo y, debajo,
// la lista de eventos del dia seleccionado.
@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private val viewModel: CalendarViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        CalendarScreen(
            state = state,
            onSelectDate = viewModel::selectDate,
            onPrevMonth = viewModel::previousMonth,
            onNextMonth = viewModel::nextMonth,
            onToday = viewModel::goToToday,
            onAddEvent = ::onAddEvent,
            onEventClick = ::onEventClick,
            onErrorShown = viewModel::errorShown,
            onShowHeatmap = ::openHeatmap,
        )
    }

    // Navega al detalle del evento (guardado contra doble navegacion).
    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.calendarFragment)
    }

    // Alta de evento desde el dia seleccionado. Pasa la fecha en el bundle (clave ARG_START_DATE) para
    // que el formulario la pre-rellene. PENDIENTE (shared): nav_graph + AddEventFragment/VM aun no
    // declaran/leen este argumento; hoy se ignora y el formulario abre con la fecha por defecto.
    private fun onAddEvent(date: LocalDate) {
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.calendarFragment) {
            controller.navigate(R.id.addEventFragment, bundleOf(ARG_START_DATE to date.toString()))
        }
    }

    private companion object {
        const val ARG_START_DATE = "startDate"
    }

    // Toggle Mes/Calor: abre el Mapa de calor (guardado contra doble navegacion desde Calendario).
    private fun openHeatmap() {
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.calendarFragment) {
            controller.navigate(R.id.heatmapFragment)
        }
    }
}
