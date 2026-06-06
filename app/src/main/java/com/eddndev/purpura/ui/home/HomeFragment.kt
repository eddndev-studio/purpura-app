package com.eddndev.purpura.ui.home

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
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentHomeBinding
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.ui.common.EventListAdapter
import com.eddndev.purpura.ui.common.navigateToEventDetail
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Pantalla principal tras autenticar (REQ-HOME-001/002). Lista los eventos de hoy + proximos 4
// dias desde el cache, con pull-to-refresh para sincronizar contra la API. El FAB crea un evento.
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()

    private val adapter = EventListAdapter(onClick = ::onEventClick)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.eventsRecycler.adapter = adapter
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.addEventFragment)
        }

        observeState()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeUiState) {
        adapter.submitList(state.events)
        binding.swipeRefresh.isRefreshing = state.isLoading
        // El RecyclerView queda siempre visible (aunque vacio) para que el pull-to-refresh
        // funcione tambien en el estado vacio: el estado vacio se muestra como overlay encima.
        binding.emptyGroup.isVisible = state.isEmpty
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    // Navega al detalle del evento (guardado contra doble navegacion).
    private fun onEventClick(event: Event) {
        findNavController().navigateToEventDetail(event.id, R.id.homeFragment)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.eventsRecycler.adapter = null
        _binding = null
    }
}
