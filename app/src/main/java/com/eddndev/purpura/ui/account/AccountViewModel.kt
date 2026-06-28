package com.eddndev.purpura.ui.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.usecase.auth.DeleteAccountUseCase
import com.eddndev.purpura.domain.usecase.auth.LinkGoogleUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.domain.usecase.auth.UnlinkGoogleUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado de la pantalla Cuenta. Dos acciones tienen progreso propio (borrado de cuenta y
// vincular/desvincular Google); cada una deshabilita la otra mientras esta en vuelo. errorRes es un
// aviso de un solo uso (snackbar). El exito del borrado NO se modela aqui: limpia la sesion y
// MainActivity navega a Auth. El exito de vincular/desvincular SI vuelve aqui (la pantalla sigue
// montada): la sesion se refresca y la fila Google cambia sola.
data class AccountUiState(
    val isDeletingAccount: Boolean = false,
    val isUpdatingGoogleLink: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

// Cuenta: hub de sesion. Expone la sesion (cabecera + estado de vinculacion Google), cierre de
// sesion, borrado de cuenta y vincular/desvincular Google. No navega por si mismo: al cerrar sesion
// o borrar, MainActivity.observeSessionGate detecta la sesion null y lleva a Auth.
@HiltViewModel
class AccountViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
    private val linkGoogle: LinkGoogleUseCase,
    private val unlinkGoogle: UnlinkGoogleUseCase,
) : ViewModel() {

    val session: StateFlow<Session?> = observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch { logout.invoke() }
    }

    // Borrado permanente. Ignora toques repetidos o si hay una vinculacion en vuelo. En exito no se
    // apaga el progreso a proposito: clear() emite la sesion null y la pantalla se desmonta. En fallo
    // se reactivan los botones y se muestra el error; la sesion se conserva intacta.
    fun deleteAccount() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isDeletingAccount = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { deleteAccount.invoke() }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isDeletingAccount = false, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    // Vincula Google con el idToken que el Fragment obtuvo del flujo de Google Sign-In. A diferencia
    // del borrado, en exito SI se apaga el progreso: la pantalla sigue montada y la sesion refrescada
    // hace que la fila pase a "vinculado".
    fun linkGoogle(idToken: String) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isUpdatingGoogleLink = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { linkGoogle.invoke(idToken) }
                .onSuccess { _uiState.update { it.copy(isUpdatingGoogleLink = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isUpdatingGoogleLink = false, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    fun unlinkGoogle() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isUpdatingGoogleLink = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { unlinkGoogle.invoke() }
                .onSuccess { _uiState.update { it.copy(isUpdatingGoogleLink = false) } }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isUpdatingGoogleLink = false, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    // El Fragment llama aqui cuando el flujo de Google Sign-In no entrega un idToken (el usuario
    // cancelo el selector, o ApiException): se muestra un aviso sin tocar la sesion ni el backend.
    fun googleSignInFailed() {
        _uiState.update { it.copy(isUpdatingGoogleLink = false, errorRes = R.string.account_link_google_failed) }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }
}

// isBusy: hay una accion con progreso propio en vuelo (borrado o vinculacion). Sirve de guard de
// re-entrada y para que cada accion bloquee a la otra mientras tanto.
private val AccountUiState.isBusy: Boolean
    get() = isDeletingAccount || isUpdatingGoogleLink
