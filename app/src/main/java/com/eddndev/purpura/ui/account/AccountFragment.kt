package com.eddndev.purpura.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.BuildConfig
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint

// Cuenta: pestana del bottom nav que agrupa lo secundario (Respaldo, Restaurar, Acerca de, vincular
// Google, Cerrar sesion, Eliminar cuenta). El Fragment monta la pantalla, resuelve la navegacion y
// conserva la plomeria de Google Sign-In (igual que AuthFragment) para obtener el idToken con el que
// VINCULAR (no iniciar sesion). El estado vive en AccountViewModel.
@AndroidEntryPoint
class AccountFragment : Fragment() {

    private val viewModel: AccountViewModel by viewModels()
    private lateinit var googleClient: GoogleSignInClient

    // El selector de Google se abre tras un signOut() asincrono; este flag evita un doble lanzamiento
    // por toques rapidos (la card sigue habilitada hasta que vuelve el idToken). Se reinicia siempre
    // que el launcher entrega un resultado (handleGoogleResult), o si el Fragment ya no esta vivo.
    private var linkInFlight = false

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> handleGoogleResult(result.data) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(requireContext(), options)

        return purpuraComposeView {
            val session by viewModel.session.collectAsStateWithLifecycle()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            AccountScreen(
                session = session,
                versionName = BuildConfig.VERSION_NAME,
                uiState = uiState,
                onBackup = { findNavController().navigate(R.id.backupFragment) },
                onRestore = { findNavController().navigate(R.id.restoreFragment) },
                onAbout = { findNavController().navigate(R.id.aboutFragment) },
                onLinkGoogle = ::launchGoogleLink,
                onUnlinkGoogle = viewModel::unlinkGoogle,
                onLogout = viewModel::logout,
                onDeleteAccount = viewModel::deleteAccount,
                onErrorShown = viewModel::errorShown,
            )
        }
    }

    // Vincular es distinto a iniciar sesion: el usuario ya esta dentro y puede tener una cuenta de
    // Google cacheada de un login previo. Se cierra esa sesion de Google ANTES de lanzar el selector
    // para que SIEMPRE pueda elegir que cuenta adjuntar (si no, signInIntent la reusaria en silencio).
    private fun launchGoogleLink() {
        if (linkInFlight) return
        linkInFlight = true
        googleClient.signOut().addOnCompleteListener {
            // El callback de la Task NO esta atado al ciclo de vida: si el Fragment se destruyo durante
            // el signOut (navegacion/rotacion), el launcher ya no esta registrado y launch() lanzaria
            // IllegalStateException. Solo se lanza si el Fragment sigue agregado.
            if (isAdded) {
                googleLauncher.launch(googleClient.signInIntent)
            } else {
                linkInFlight = false
            }
        }
    }

    private fun handleGoogleResult(data: Intent?) {
        linkInFlight = false
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.linkGoogle(idToken)
            } else {
                viewModel.googleSignInFailed()
            }
        } catch (error: ApiException) {
            viewModel.googleSignInFailed()
        }
    }
}
