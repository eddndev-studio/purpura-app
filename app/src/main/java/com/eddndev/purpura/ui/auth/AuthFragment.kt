package com.eddndev.purpura.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentAuthBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

// Punto de entrada cuando no hay sesion (06-app-architecture §13.1). Inicio de sesion /
// registro con correo y contrasena, o Google Sign-In. Al persistir la sesion, MainActivity
// navega a Inicio; esta pantalla no navega por si misma.
@AndroidEntryPoint
class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()

    private var isRegister = false
    private lateinit var googleClient: GoogleSignInClient

    private val googleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result -> handleGoogleResult(result.data) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.server_client_id))
            .requestEmail()
            .build()
        googleClient = GoogleSignIn.getClient(requireContext(), options)

        binding.submitButton.setOnClickListener { submit() }
        binding.toggleButton.setOnClickListener { toggleMode() }
        binding.googleButton.setOnClickListener { googleLauncher.launch(googleClient.signInIntent) }

        observeState()
    }

    private fun toggleMode() {
        isRegister = !isRegister
        binding.nameInputLayout.isVisible = isRegister
        binding.submitButton.setText(if (isRegister) R.string.auth_register else R.string.auth_login)
        binding.toggleButton.setText(
            if (isRegister) R.string.auth_toggle_to_login else R.string.auth_toggle_to_register,
        )
    }

    private fun submit() {
        val email = binding.emailInput.text?.toString()?.trim().orEmpty()
        val password = binding.passwordInput.text?.toString().orEmpty()
        val name = binding.nameInput.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || password.isEmpty() || (isRegister && name.isEmpty())) {
            Snackbar.make(binding.root, R.string.auth_error_empty_fields, Snackbar.LENGTH_SHORT).show()
            return
        }
        if (isRegister) viewModel.register(email, name, password) else viewModel.login(email, password)
    }

    private fun handleGoogleResult(data: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data)
                .getResult(ApiException::class.java)
            val idToken = account.idToken
            if (idToken != null) {
                viewModel.signInWithGoogle(idToken)
            } else {
                Snackbar.make(binding.root, R.string.auth_google_cancelled, Snackbar.LENGTH_SHORT).show()
            }
        } catch (error: ApiException) {
            Snackbar.make(binding.root, R.string.auth_google_cancelled, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: AuthUiState) {
        val loading = state is AuthUiState.Loading
        binding.progressBar.isVisible = loading
        binding.submitButton.isEnabled = !loading
        binding.googleButton.isEnabled = !loading
        binding.toggleButton.isEnabled = !loading
        if (state is AuthUiState.Error) {
            Snackbar.make(binding.root, state.messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
