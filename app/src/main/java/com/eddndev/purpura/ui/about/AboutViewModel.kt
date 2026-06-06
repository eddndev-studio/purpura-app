package com.eddndev.purpura.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// Acerca de (REQ-AUTH-004). Solo expone el cierre de sesion: borra token + cache via LogoutUseCase.
// No navega: al limpiarse la sesion, MainActivity.observeSessionGate detecta el null y lleva a Auth.
@HiltViewModel
class AboutViewModel @Inject constructor(
    private val logout: LogoutUseCase,
) : ViewModel() {

    fun logout() {
        viewModelScope.launch { logout.invoke() }
    }
}
