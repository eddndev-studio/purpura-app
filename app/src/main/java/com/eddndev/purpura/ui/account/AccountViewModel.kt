package com.eddndev.purpura.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Cuenta: hub de sesion. Expone la sesion (para nombre/email en la cabecera) y el cierre de sesion.
// No navega al cerrar: MainActivity.observeSessionGate detecta el null y lleva a Auth.
@HiltViewModel
class AccountViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logout: LogoutUseCase,
) : ViewModel() {

    val session: StateFlow<Session?> = observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logout() {
        viewModelScope.launch { logout.invoke() }
    }
}
