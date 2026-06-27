package com.eddndev.purpura.ui.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.usecase.auth.DeleteAccountUseCase
import com.eddndev.purpura.domain.usecase.auth.LogoutUseCase
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
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

// Estado del borrado de cuenta: deshabilita los botones e indica progreso mientras la llamada
// esta en vuelo; errorRes es un aviso de un solo uso (snackbar). El exito NO se modela aqui:
// limpia la sesion y MainActivity navega a Auth, desmontando la pantalla.
data class AccountUiState(
    val isDeletingAccount: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

// Cuenta: hub de sesion. Expone la sesion (para nombre/email en la cabecera), el cierre de sesion
// y el borrado de cuenta. No navega: MainActivity.observeSessionGate detecta el null (al cerrar
// sesion o al borrar) y lleva a Auth.
@HiltViewModel
class AccountViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    val session: StateFlow<Session?> = observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    fun logout() {
        viewModelScope.launch { logout.invoke() }
    }

    // Borrado permanente. Ignora toques repetidos mientras hay uno en vuelo. En exito no se apaga
    // el progreso a proposito: clear() emite la sesion null y la pantalla se desmonta. En fallo se
    // reactivan los botones y se muestra el error; la sesion se conserva intacta.
    fun deleteAccount() {
        if (_uiState.value.isDeletingAccount) return
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

    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }
}
