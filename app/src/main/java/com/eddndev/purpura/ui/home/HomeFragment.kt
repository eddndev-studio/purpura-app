package com.eddndev.purpura.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentHomeBinding

// Pantalla principal tras autenticar. En el esqueleto muestra el estado vacio y el
// FAB "Anadir Eventos"; en #8 listara los eventos de hoy + proximos 4 dias.
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

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
        binding.fabAddEvent.setOnClickListener {
            findNavController().navigate(R.id.addEventFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
