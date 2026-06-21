package com.eddndev.purpura.ui.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.BuildConfig
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.purpuraComposeView
import dagger.hilt.android.AndroidEntryPoint

// Cuenta: pestana del bottom nav que agrupa lo secundario (Respaldo, Restaurar, Acerca de, Cerrar
// sesion). El Fragment solo monta la pantalla y resuelve la navegacion; el estado vive en
// AccountViewModel. Sustituye al antiguo Navigation Drawer.
@AndroidEntryPoint
class AccountFragment : Fragment() {

    private val viewModel: AccountViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val session by viewModel.session.collectAsStateWithLifecycle()
        AccountScreen(
            session = session,
            versionName = BuildConfig.VERSION_NAME,
            onBackup = { findNavController().navigate(R.id.backupFragment) },
            onRestore = { findNavController().navigate(R.id.restoreFragment) },
            onAbout = { findNavController().navigate(R.id.aboutFragment) },
            onLogout = viewModel::logout,
        )
    }
}
