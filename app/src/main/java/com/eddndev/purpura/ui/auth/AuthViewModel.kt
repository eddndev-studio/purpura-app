package com.eddndev.purpura.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.usecase.auth.GoogleSignInUseCase
import com.eddndev.purpura.domain.usecase.auth.LoginUseCase
import com.eddndev.purpura.domain.usecase.auth.RegisterUseCase
import com.eddndev.purpura.ui.common.toMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Coordina el inicio de sesion / registro / Google. Persiste la sesion via los casos de uso;
// la navegacion a Inicio la dispara MainActivity al observar la sesion.
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) = launchAuth {
        loginUseCase(email, password)
    }

    fun register(email: String, nombre: String, password: String) = launchAuth {
        registerUseCase(email, nombre, password)
    }

    fun signInWithGoogle(idToken: String) = launchAuth {
        googleSignInUseCase(idToken)
    }

    fun errorShown() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    private fun launchAuth(block: suspend () -> Unit) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            runCatching { block() }
                .onSuccess { _uiState.value = AuthUiState.Idle }
                .onFailure { throwable ->
                    val error = throwable as? DomainError ?: DomainError.Unexpected(throwable)
                    _uiState.value = AuthUiState.Error(error.toMessageRes())
                }
        }
    }
}
