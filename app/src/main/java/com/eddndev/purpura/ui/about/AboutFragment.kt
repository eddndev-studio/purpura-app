package com.eddndev.purpura.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentAboutBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

// Acerca de: identidad de la app + cierre de sesion (REQ-AUTH-004). El logout borra token + cache;
// la navegacion de vuelta a Auth la hace MainActivity al observar la sesion en null (no aqui).
@AndroidEntryPoint
class AboutFragment : Fragment() {

    private var _binding: FragmentAboutBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAboutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.logoutButton.setOnClickListener { confirmLogout() }
    }

    // Confirma antes de cerrar sesion (mismo idioma que el dialogo de Salir). Al confirmar, el VM
    // limpia la sesion y el gate de MainActivity navega a Auth.
    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about_logout_title)
            .setMessage(R.string.about_logout_message)
            .setNegativeButton(R.string.about_logout_cancel, null)
            .setPositiveButton(R.string.about_logout_confirm) { _, _ -> viewModel.logout() }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
