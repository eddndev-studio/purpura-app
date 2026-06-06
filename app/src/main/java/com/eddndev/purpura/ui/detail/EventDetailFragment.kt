package com.eddndev.purpura.ui.detail

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentEventDetailBinding
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import com.eddndev.purpura.ui.common.EventDisplay
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Detalle y edicion de un evento (REQ-QUERY-007..013). Carga por id (argumento de navegacion),
// muestra los campos, permite cambiar el estatus (chips) y eliminar (con confirmacion). El id se
// entrega al VM con load() para mantenerlo testeable sin SavedStateHandle.
@AndroidEntryPoint
class EventDetailFragment : Fragment() {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DetailViewModel by viewModels()

    private var suppressStatusEvents = false
    private var handledDeleted = false

    // Mapa lite de la ubicacion (solo lectura). Se crea perezosamente la primera vez que un evento
    // trae coordenadas reales.
    private var detailMap: GoogleMap? = null
    private var mapLatLng: LatLng? = null
    private var mapRequested = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getString(ARG_EVENT_ID).orEmpty()
        if (eventId.isBlank()) {
            findNavController().navigateUp()
            return
        }

        binding.statusChipGroup.setOnCheckedStateChangeListener { _, _ ->
            if (!suppressStatusEvents) {
                checkedStatus()?.let(viewModel::changeStatus)
            }
        }
        binding.deleteButton.setOnClickListener { confirmDelete() }
        binding.retryButton.setOnClickListener { viewModel.retry() }

        observeState()
        viewModel.load(eventId)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: DetailUiState) {
        if (state.deleted && !handledDeleted) {
            handledDeleted = true
            findNavController().navigateUp()
            return
        }

        binding.progressBar.isVisible = state.isLoading
        binding.contentScroll.isVisible = state.event != null
        binding.errorGroup.isVisible = state.loadFailed && state.event == null && !state.isLoading
        binding.workingProgress.isVisible = state.isWorking

        state.event?.let { event ->
            bindEvent(event)
            // El estatus se sincroniza desde el evento (fuente de verdad) solo cuando no hay una
            // operacion en vuelo: asi un cambio fallido revierte el chip y no parpadea durante la
            // operacion.
            if (!state.isWorking) setStatusChip(event.status)
        }

        val controlsEnabled = !state.isWorking
        binding.deleteButton.isEnabled = controlsEnabled
        binding.statusChipGroup.children.forEach { it.isEnabled = controlsEnabled }

        state.errorRes?.let { messageRes ->
            // Un fallo de carga (sin evento) ya se comunica con el estado de error + reintentar; el
            // snackbar queda para errores sobre un evento visible (cambio de estatus / eliminar).
            if (state.event != null) {
                Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            }
            viewModel.errorShown()
        }
    }

    private fun bindEvent(event: Event) {
        val context = binding.root.context
        binding.dateFullText.text = EventDisplay.formatFullDate(event.startsAt)
        binding.timeText.text = EventDisplay.formatTime(event.startsAt)
        binding.descriptionText.text = event.description
        binding.contactText.text = event.contact.name
        binding.locationText.text = event.location.label?.takeIf { it.isNotBlank() }
            ?: getString(R.string.detail_no_location)
        updateLocationMap(event.location)
        binding.reminderText.setText(EventDisplay.reminderLabel(event.reminder))

        binding.typeBadge.apply {
            setText(EventDisplay.typeLabel(event.type))
            setTextColor(ContextCompat.getColor(context, EventDisplay.typeColor(event.type)))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, EventDisplay.typeContainer(event.type)),
            )
        }
        binding.statusBadge.apply {
            setText(EventDisplay.statusLabel(event.status))
            setTextColor(ContextCompat.getColor(context, EventDisplay.statusColor(event.status)))
            backgroundTintList = ColorStateList.valueOf(
                ContextCompat.getColor(context, EventDisplay.statusContainer(event.status)),
            )
        }
    }

    private fun setStatusChip(status: EventStatus) {
        suppressStatusEvents = true
        val chipId = when (status) {
            EventStatus.pendiente -> R.id.chipDetailPendiente
            EventStatus.realizado -> R.id.chipDetailRealizado
            EventStatus.aplazado -> R.id.chipDetailAplazado
        }
        binding.statusChipGroup.check(chipId)
        suppressStatusEvents = false
    }

    private fun checkedStatus(): EventStatus? = when (binding.statusChipGroup.checkedChipId) {
        R.id.chipDetailPendiente -> EventStatus.pendiente
        R.id.chipDetailRealizado -> EventStatus.realizado
        R.id.chipDetailAplazado -> EventStatus.aplazado
        else -> null
    }

    private fun confirmDelete() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.detail_delete_confirm_title)
            .setMessage(R.string.detail_delete_confirm_message)
            .setNegativeButton(R.string.detail_delete_cancel, null)
            .setPositiveButton(R.string.detail_delete_confirm) { _, _ -> viewModel.delete() }
            .show()
    }

    // Muestra el mapa lite solo si el evento tiene coordenadas reales. lat==0 && lng==0 es el
    // centinela de "sin ubicacion en mapa" (eventos viejos solo-etiqueta): no es limpio, pero evita
    // re-modelar Location por ahora.
    private fun updateLocationMap(location: Location) {
        val hasCoordinates = location.lat != 0.0 || location.lng != 0.0
        binding.detailMapCard.isVisible = hasCoordinates
        if (!hasCoordinates) return

        mapLatLng = LatLng(location.lat, location.lng)
        detailMap?.let {
            applyMapMarker()
            return
        }
        if (mapRequested) return
        mapRequested = true
        val mapFragment = SupportMapFragment.newInstance(GoogleMapOptions().liteMode(true))
        childFragmentManager.beginTransaction()
            .replace(R.id.detailMapContainer, mapFragment)
            .commitNow()
        mapFragment.getMapAsync { map ->
            if (_binding == null) return@getMapAsync
            detailMap = map
            applyMapMarker()
        }
    }

    private fun applyMapMarker() {
        val map = detailMap ?: return
        val latLng = mapLatLng ?: return
        map.clear()
        map.addMarker(MarkerOptions().position(latLng))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DETAIL_MAP_ZOOM))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        detailMap = null
        mapRequested = false
        _binding = null
    }

    private companion object {
        const val DETAIL_MAP_ZOOM = 15f
    }
}
