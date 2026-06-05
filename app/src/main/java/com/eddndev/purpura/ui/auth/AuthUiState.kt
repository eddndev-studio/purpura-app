package com.eddndev.purpura.ui.auth

import androidx.annotation.StringRes

// Estado de la pantalla de autenticacion. El exito no es un estado: al persistir la sesion,
// MainActivity detecta la sesion y navega a Inicio (06-app-architecture §8.1).
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Error(@StringRes val messageRes: Int) : AuthUiState
}
