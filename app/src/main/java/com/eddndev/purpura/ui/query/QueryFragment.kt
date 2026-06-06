package com.eddndev.purpura.ui.query

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentQueryBinding
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.common.EventListAdapter
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Consultar (REQ-QUERY-001..006). Filtros por periodo/tipo/estatus que aplican al cambiar; el
// modo Dia/Rango abre un selector de fecha. La lista reusa EventListAdapter, pagina al hacer
// scroll y navega al Detalle al tocar una tarjeta.
//
// Alcance v1: modos Todos/Dia/Rango. Mes/Ano se agregan en un commit posterior (EventQuery y el
// caso de uso ya los soportan).
@AndroidEntryPoint
class QueryFragment : Fragment() {

    private var _binding: FragmentQueryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QueryViewModel by viewModels()

    private val adapter = EventListAdapter(onClick = ::onEventClick)

    // Fechas elegidas por el usuario. El modo solo se considera activo si su(s) fecha(s) existen,
    // de modo que filtrar por tipo/estatus nunca dispara una consulta temporal incompleta.
    private var selectedDate: LocalDate? = null
    private var selectedFrom: LocalDate? = null
    private var selectedTo: LocalDate? = null

    // Suprime la reaccion a cambios de chip cuando NO los origina el usuario: al revertir un chip
    // por codigo (cancelar el selector) y durante la restauracion de la vista (rotacion / muerte de
    // proceso), donde el framework re-marca los chips y dispararia busquedas/selectores espurios.
    private var suppressChipEvents = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentQueryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Suprimido hasta que termine la restauracion de la vista (onViewStateRestored): los chips
        // se re-marcan ahi y no deben disparar busqueda ni abrir el selector.
        suppressChipEvents = true

        binding.resultsRecycler.adapter = adapter
        setupPaging()

        binding.modeChipGroup.setOnCheckedStateChangeListener { _, _ ->
            if (!suppressChipEvents) onModeChanged()
        }
        binding.typeChipGroup.setOnCheckedStateChangeListener { _, _ ->
            if (!suppressChipEvents) applyFilters()
        }
        binding.statusChipGroup.setOnCheckedStateChangeListener { _, _ ->
            if (!suppressChipEvents) applyFilters()
        }
        binding.dateButton.setOnClickListener { openDatePickerForMode() }

        observeState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Tras restaurar la vista: descarta un selector huerfano (sus listeners no sobreviven a la
        // recreacion) y re-sincroniza el sub-estado de fecha desde el VM, que es la fuente de verdad
        // de los filtros (los chips se restauran solos, pero las fechas elegidas no). Recien aqui se
        // habilitan de nuevo los eventos de chip.
        (parentFragmentManager.findFragmentByTag(PICKER_TAG) as? DialogFragment)?.dismiss()
        restoreDateUiFromFilters()
        suppressChipEvents = false
    }

    // Reconstruye selectedDate/from/to y el boton de fecha a partir de los filtros que conserva el
    // VM y del chip de modo ya restaurado. Si el modo quedo activo sin fecha (rotacion con el
    // selector abierto), el boton muestra su texto por defecto para que el usuario lo reabra.
    private fun restoreDateUiFromFilters() {
        val filters = viewModel.uiState.value.filters
        selectedDate = filters.date
        selectedFrom = filters.from
        selectedTo = filters.to
        when (binding.modeChipGroup.checkedChipId) {
            R.id.chipModeDay -> {
                binding.dateButton.isVisible = true
                binding.dateButton.text = selectedDate?.let(dateLabelFormat::format)
                    ?: getString(R.string.query_pick_day)
            }
            R.id.chipModeRange -> {
                binding.dateButton.isVisible = true
                binding.dateButton.text = if (selectedFrom != null && selectedTo != null) {
                    getString(
                        R.string.query_range_format,
                        dateLabelFormat.format(selectedFrom),
                        dateLabelFormat.format(selectedTo),
                    )
                } else {
                    getString(R.string.query_pick_range)
                }
            }
            else -> binding.dateButton.isVisible = false
        }
    }

    private fun onModeChanged() {
        when (binding.modeChipGroup.checkedChipId) {
            R.id.chipModeDay -> {
                binding.dateButton.setText(R.string.query_pick_day)
                binding.dateButton.isVisible = true
                openSingleDatePicker()
            }
            R.id.chipModeRange -> {
                binding.dateButton.setText(R.string.query_pick_range)
                binding.dateButton.isVisible = true
                openRangePicker()
            }
            else -> {
                binding.dateButton.isVisible = false
                clearDates()
                applyFilters()
            }
        }
    }

    private fun openDatePickerForMode() {
        when (binding.modeChipGroup.checkedChipId) {
            R.id.chipModeDay -> openSingleDatePicker()
            R.id.chipModeRange -> openRangePicker()
        }
    }

    private fun openSingleDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.query_date_picker_title)
            .setSelection(selectedDate?.toUtcMillis() ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = millis.toLocalDateUtc()
            binding.dateButton.text = dateLabelFormat.format(selectedDate)
            applyFilters()
        }
        picker.addOnNegativeButtonClickListener { revertToAllIfNoDay() }
        picker.addOnCancelListener { revertToAllIfNoDay() }
        picker.show(parentFragmentManager, PICKER_TAG)
    }

    private fun openRangePicker() {
        val seed = if (selectedFrom != null && selectedTo != null) {
            androidx.core.util.Pair(selectedFrom!!.toUtcMillis(), selectedTo!!.toUtcMillis())
        } else {
            null
        }
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.query_range_picker_title)
            .apply { if (seed != null) setSelection(seed) }
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            selectedFrom = range.first?.toLocalDateUtc()
            selectedTo = range.second?.toLocalDateUtc()
            binding.dateButton.text = getString(
                R.string.query_range_format,
                selectedFrom?.let(dateLabelFormat::format).orEmpty(),
                selectedTo?.let(dateLabelFormat::format).orEmpty(),
            )
            applyFilters()
        }
        picker.addOnNegativeButtonClickListener { revertToAllIfNoRange() }
        picker.addOnCancelListener { revertToAllIfNoRange() }
        picker.show(parentFragmentManager, PICKER_TAG)
    }

    // Si el usuario abrio el selector pero no habia una fecha previa y cancelo, volvemos a Todos
    // para no dejar el modo activo sin fecha.
    private fun revertToAllIfNoDay() {
        if (selectedDate == null) checkModeAll()
    }

    private fun revertToAllIfNoRange() {
        if (selectedFrom == null || selectedTo == null) checkModeAll()
    }

    private fun checkModeAll() {
        suppressChipEvents = true
        binding.chipModeAll.isChecked = true
        suppressChipEvents = false
        binding.dateButton.isVisible = false
        clearDates()
        applyFilters()
    }

    private fun clearDates() {
        selectedDate = null
        selectedFrom = null
        selectedTo = null
    }

    private fun applyFilters() {
        // El modo solo cuenta como activo si tiene su(s) fecha(s); si no, equivale a "Todos".
        val mode = when (binding.modeChipGroup.checkedChipId) {
            R.id.chipModeDay -> if (selectedDate != null) QueryMode.por_dia else null
            R.id.chipModeRange -> if (selectedFrom != null && selectedTo != null) QueryMode.por_rango else null
            else -> null
        }
        viewModel.search(
            QueryFilters(
                mode = mode,
                type = selectedType(),
                status = selectedStatus(),
                date = selectedDate.takeIf { mode == QueryMode.por_dia },
                from = selectedFrom.takeIf { mode == QueryMode.por_rango },
                to = selectedTo.takeIf { mode == QueryMode.por_rango },
            ),
        )
    }

    private fun selectedType(): EventType? = when (binding.typeChipGroup.checkedChipId) {
        R.id.chipTypeCita -> EventType.cita
        R.id.chipTypeJunta -> EventType.junta
        R.id.chipTypeEntrega -> EventType.entrega_proyecto
        R.id.chipTypeExamen -> EventType.examen
        R.id.chipTypeOtros -> EventType.otros
        else -> null
    }

    private fun selectedStatus(): EventStatus? = when (binding.statusChipGroup.checkedChipId) {
        R.id.chipStatusPendiente -> EventStatus.pendiente
        R.id.chipStatusRealizado -> EventStatus.realizado
        R.id.chipStatusAplazado -> EventStatus.aplazado
        else -> null
    }

    private fun setupPaging() {
        binding.resultsRecycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy <= 0) return
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                if (lastVisible >= layoutManager.itemCount - PAGING_THRESHOLD) {
                    viewModel.loadMore()
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: QueryUiState) {
        adapter.submitList(state.events)
        binding.progressBar.isVisible = state.isLoading && state.events.isEmpty()
        binding.pagingProgress.isVisible = state.isPaging
        binding.emptyGroup.isVisible = state.isEmpty
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.queryFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.resultsRecycler.adapter = null
        _binding = null
    }

    // MaterialDatePicker opera en UTC (medianoche): convertimos a/desde LocalDate con UTC.
    private fun Long.toLocalDateUtc(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    private fun LocalDate.toUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val PAGING_THRESHOLD = 3
        const val PICKER_TAG = "query_date_picker"
        val dateLabelFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX"))
    }
}
