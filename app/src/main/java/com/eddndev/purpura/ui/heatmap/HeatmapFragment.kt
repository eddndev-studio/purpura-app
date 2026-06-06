package com.eddndev.purpura.ui.heatmap

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
import androidx.recyclerview.widget.GridLayoutManager
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentHeatmapBinding
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.common.MonthGrid
import com.eddndev.purpura.ui.common.bindWeekdayHeader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

// Mapa de calor mensual (densidad de eventos por dia). Pinta la rejilla de mosaicos tenidos por
// intensidad; tocar un dia muestra su conteo en un Snackbar. El estado viene de HeatmapViewModel.
@AndroidEntryPoint
class HeatmapFragment : Fragment() {

    private var _binding: FragmentHeatmapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HeatmapViewModel by viewModels()

    private val dayAdapter = HeatmapDayAdapter(onDayClick = ::onCellClick)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHeatmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.heatmapRecycler.layoutManager =
            GridLayoutManager(requireContext(), MonthGrid.DAYS_PER_WEEK)
        binding.heatmapRecycler.adapter = dayAdapter

        binding.prevMonthButton.setOnClickListener { viewModel.previousMonth() }
        binding.nextMonthButton.setOnClickListener { viewModel.nextMonth() }

        binding.weekdayHeader.bindWeekdayHeader(LOCALE)
        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HeatmapUiState) {
        binding.monthLabel.text = EventDisplay.formatMonth(state.yearMonth)
        binding.loadingBar.isVisible = state.isLoading
        dayAdapter.submit(state.cells)
        binding.heatmapSummary.text = resources.getQuantityString(
            R.plurals.heatmap_month_total,
            state.totalEvents,
            state.totalEvents,
        )

        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    // Tocar un dia: muestra su conteo de eventos sin salir de la pantalla (vista general).
    private fun onCellClick(cell: HeatmapCell.Day) {
        val message = resources.getQuantityString(
            R.plurals.heatmap_day_count,
            cell.count,
            cell.count,
            EventDisplay.formatFullDate(cell.date),
        )
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.heatmapRecycler.adapter = null
        _binding = null
    }

    private companion object {
        val LOCALE: Locale = Locale("es", "MX")
    }
}
