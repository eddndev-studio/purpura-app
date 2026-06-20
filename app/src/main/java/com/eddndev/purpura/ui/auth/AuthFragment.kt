package com.eddndev.purpura.ui.auth

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
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint

// Punto de entrada cuando no hay sesion (06-app-architecture §13.1). Migrada a Compose: el Fragment
// solo monta AuthScreen y conserva la plomeria de Google Sign-In (launcher de ActivityResult,
// construccion de GoogleSignInOptions y handleGoogleResult), pasando onGoogleClick como callback. El
// modo login/registro y la validacion local viven en la pantalla. Al persistir la sesion, MainActivity
// navega a Inicio; esta pantalla no navega por si misma.
@AndroidEntryPoint
class AuthFragment : Fragment() {

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleClient: GoogleSignInClient

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
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            AuthScreen(
                state = state,
                onLogin = viewModel::login,
                onRegister = viewModel::register,
                onGoogleClick = { googleLauncher.launch(googleClient.signInIntent) },
                onErrorShown = viewModel::errorShown,
            )
        }
    }

    private fun handleGoogleResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Snackbar.make(requireView(), R.string.auth_google_cancelled, Snackbar.LENGTH_SHORT).show()
            }
        } catch (error: ApiException) {
            Snackbar.make(requireView(), R.string.auth_google_cancelled, Snackbar.LENGTH_SHORT).show()
        }
    }
}
