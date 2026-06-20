package com.eddndev.purpura.ui.query

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.QueryMode
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.google.android.material.datepicker.MaterialDatePicker
import dagger.hilt.android.AndroidEntryPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

// Consultar (REQ-QUERY-001..006). Migrada a Compose (QueryScreen): el Fragment solo monta la
// pantalla, resuelve la navegacion al Detalle y CONSERVA los selectores de fecha (MaterialDatePicker
// para Dia/Rango y MonthYearPicker para Mes/Ano), que Compose no implementa. Cuando QueryScreen
// elige un modo temporal o toca el boton de fecha, invoca onPickDate(mode): aqui abrimos el selector
// adecuado y, al elegir, llamamos a viewModel.search con el filtro completo. Cancelar sin fecha
// vuelve a "Todos" (mismo criterio que la version XML). El estado vive en QueryViewModel.
@AndroidEntryPoint
class QueryFragment : Fragment() {

    private val viewModel: QueryViewModel by viewModels()

    // Dialogo de Mes/Ano abierto (no es un DialogFragment): lo descartamos en onDestroyView para no
    // filtrar la ventana al rotar. Ese dismiss programatico no dispara onCancel (ver MonthYearPicker).
    private var periodDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        QueryScreen(
            state = state,
            onSearch = viewModel::search,
            onLoadMore = viewModel::loadMore,
            onPickDate = ::openDatePickerForMode,
            onEventClick = ::onEventClick,
            onErrorShown = viewModel::errorShown,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Tras una recreacion, un selector huerfano no conserva sus listeners: lo descartamos para
        // que el usuario lo reabra desde el boton de fecha (ya re-sincronizado por el estado del VM).
        (parentFragmentManager.findFragmentByTag(PICKER_TAG) as? DialogFragment)?.dismiss()
    }

    // Abre el selector segun el modo. La fuente de verdad de los filtros es el VM: de ahi sembramos
    // las fechas previas y preservamos tipo/estatus al construir el filtro resultante.
    private fun openDatePickerForMode(mode: QueryMode) {
        when (mode) {
            QueryMode.por_dia -> openSingleDatePicker()
            QueryMode.por_rango -> openRangePicker()
            QueryMode.por_mes -> openMonthPicker()
            QueryMode.por_anio -> openYearPicker()
        }
    }

    private fun currentFilters(): QueryFilters = viewModel.uiState.value.filters

    private fun openSingleDatePicker() {
        val current = currentFilters()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.query_date_picker_title)
            .setSelection(current.date?.toUtcMillis() ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val date = millis.toLocalDateUtc()
            viewModel.search(currentFilters().copy(mode = QueryMode.por_dia, date = date, from = null, to = null, year = null, month = null))
        }
        picker.addOnNegativeButtonClickListener { revertToAllIfNoMode(QueryMode.por_dia) }
        picker.addOnCancelListener { revertToAllIfNoMode(QueryMode.por_dia) }
        picker.show(parentFragmentManager, PICKER_TAG)
    }

    private fun openRangePicker() {
        val current = currentFilters()
        val seed = if (current.from != null && current.to != null) {
            androidx.core.util.Pair(current.from!!.toUtcMillis(), current.to!!.toUtcMillis())
        } else {
            null
        }
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.query_range_picker_title)
            .apply { if (seed != null) setSelection(seed) }
            .build()
        picker.addOnPositiveButtonClickListener { range ->
            val from = range.first?.toLocalDateUtc()
            val to = range.second?.toLocalDateUtc()
            viewModel.search(
                currentFilters().copy(
                    mode = QueryMode.por_rango, from = from, to = to,
                    date = null, year = null, month = null,
                ),
            )
        }
        picker.addOnNegativeButtonClickListener { revertToAllIfNoMode(QueryMode.por_rango) }
        picker.addOnCancelListener { revertToAllIfNoMode(QueryMode.por_rango) }
        picker.show(parentFragmentManager, PICKER_TAG)
    }

    private fun openMonthPicker() {
        val current = currentFilters()
        val today = LocalDate.now()
        periodDialog = MonthYearPicker.showMonth(
            context = requireContext(),
            initialYear = current.year ?: today.year,
            initialMonth = current.month ?: today.monthValue,
            onPicked = { year, month ->
                viewModel.search(
                    currentFilters().copy(
                        mode = QueryMode.por_mes, year = year, month = month,
                        date = null, from = null, to = null,
                    ),
                )
            },
            onCancel = { revertToAllIfNoMode(QueryMode.por_mes) },
        )
    }

    private fun openYearPicker() {
        val current = currentFilters()
        val today = LocalDate.now()
        periodDialog = MonthYearPicker.showYear(
            context = requireContext(),
            initialYear = current.year ?: today.year,
            onPicked = { year ->
                viewModel.search(
                    currentFilters().copy(
                        mode = QueryMode.por_anio, year = year, month = null,
                        date = null, from = null, to = null,
                    ),
                )
            },
            onCancel = { revertToAllIfNoMode(QueryMode.por_anio) },
        )
    }

    // Si el usuario abrio el selector pero no llego a fijar la(s) fecha(s) del modo, volvemos a
    // "Todos" preservando tipo/estatus, para no quedar con un modo activo sin fecha.
    private fun revertToAllIfNoMode(mode: QueryMode) {
        val filters = currentFilters()
        val complete = when (mode) {
            QueryMode.por_dia -> filters.mode == QueryMode.por_dia && filters.date != null
            QueryMode.por_rango -> filters.mode == QueryMode.por_rango && filters.from != null && filters.to != null
            QueryMode.por_mes -> filters.mode == QueryMode.por_mes && filters.year != null && filters.month != null
            QueryMode.por_anio -> filters.mode == QueryMode.por_anio && filters.year != null
        }
        if (!complete) {
            viewModel.search(QueryFilters(type = filters.type, status = filters.status))
        }
    }

    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.queryFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        periodDialog?.dismiss()
        periodDialog = null
    }

    // MaterialDatePicker opera en UTC (medianoche): convertimos a/desde LocalDate con UTC.
    private fun Long.toLocalDateUtc(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()

    private fun LocalDate.toUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val PICKER_TAG = "query_date_picker"
    }
}
