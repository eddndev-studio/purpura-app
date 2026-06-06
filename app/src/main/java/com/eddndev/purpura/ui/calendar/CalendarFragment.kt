package com.eddndev.purpura.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentCalendarBinding
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.EventListAdapter
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.common.addHeatmapMenu
import com.eddndev.purpura.ui.common.bindWeekdayHeader
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

// Calendario mensual (REQ-CAL-001..003). Pinta la rejilla del mes con puntos por tipo y, debajo,
// la lista de eventos del dia seleccionado (reusa EventListAdapter -> Detalle). El estado viene de
// CalendarViewModel; aqui solo se enlazan vistas. El encabezado de dias se genera segun el locale.
@AndroidEntryPoint
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CalendarViewModel by viewModels()

    private val dayAdapter = CalendarDayAdapter(onDayClick = ::onDaySelected)
    private val eventAdapter = EventListAdapter(onClick = ::onEventClick)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.calendarRecycler.layoutManager =
            GridLayoutManager(requireContext(), MonthGrid.DAYS_PER_WEEK)
        binding.calendarRecycler.adapter = dayAdapter
        binding.selectedDayRecycler.adapter = eventAdapter

        binding.prevMonthButton.setOnClickListener { viewModel.previousMonth() }
        binding.nextMonthButton.setOnClickListener { viewModel.nextMonth() }

        binding.weekdayHeader.bindWeekdayHeader(LOCALE)
        addHeatmapMenu(R.id.calendarFragment)
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: CalendarUiState) {
        binding.monthLabel.text = EventDisplay.formatMonth(state.yearMonth)
        binding.loadingBar.isVisible = state.isLoading
        dayAdapter.submit(state.cells)
        binding.selectedDayLabel.text =
            state.selectedDate?.let(EventDisplay::formatFullDate).orEmpty()
        eventAdapter.submitList(state.selectedDayEvents)
        binding.emptyDayText.isVisible = state.selectedDayEvents.isEmpty() && !state.isLoading

        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    private fun onDaySelected(date: LocalDate) {
        viewModel.selectDate(date)
    }

    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.calendarFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.calendarRecycler.adapter = null
        binding.selectedDayRecycler.adapter = null
        _binding = null
    }

    private companion object {
        val LOCALE: Locale = Locale("es", "MX")
    }
}
