package com.eddndev.purpura.ui.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint

// Acerca de: identidad de la app + cierre de sesion (REQ-AUTH-004). Migrada a Compose (AboutScreen).
// El logout borra token + cache; la navegacion de vuelta a Auth la hace MainActivity al observar la
// sesion en null (no aqui).
@AndroidEntryPoint
class AboutFragment : Fragment() {

    private val viewModel: AboutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        AboutScreen(onLogout = viewModel::logout)
    }
}
