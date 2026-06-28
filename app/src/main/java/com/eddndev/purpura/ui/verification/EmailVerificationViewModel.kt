package com.eddndev.purpura.ui.verification

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.domain.usecase.auth.RefreshCurrentUserUseCase
import com.eddndev.purpura.domain.usecase.auth.RequestEmailVerificationUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estado del aviso "verifica tu correo" en Inicio. `visible` lo decide la sesion (usuario sin
// verificar); `sent` cambia el copy a "revisa tu bandeja" tras enviar; `errorRes` es un aviso inline
// de un solo uso. El banner es autocontenido: no usa el snackbar de Inicio.
data class EmailVerificationUiState(
    val visible: Boolean = false,
    val email: String = "",
    val isSending: Boolean = false,
    val sent: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

// Gobierna el aviso de verificacion de correo. Observa la sesion para decidir si mostrarlo
// (emailVerified=false), dispara el envio del correo (POST /auth/verify-email/request) y refresca el
// usuario al reanudar (GET /auth/me) para que el aviso desaparezca solo cuando el usuario confirma en
// el navegador. No navega ni cierra sesion.
@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val requestEmailVerification: RequestEmailVerificationUseCase,
    private val refreshCurrentUser: RefreshCurrentUserUseCase,
) : ViewModel() {

    // Estado propio de la accion de enviar; se combina con el usuario de la sesion para el UI state.
    private val action = MutableStateFlow(ActionState())

    // Job del refresh en vuelo: se cancela al lanzar otro para que gane el /auth/me mas reciente.
    private var refreshJob: Job? = null

    val uiState: StateFlow<EmailVerificationUiState> = combine(
        observeSession(),
        action,
    ) { session, act ->
        val user = session?.user
        EmailVerificationUiState(
            visible = user != null && !user.emailVerified,
            email = user?.email.orEmpty(),
            isSending = act.isSending,
            sent = act.sent,
            errorRes = act.errorRes,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EmailVerificationUiState())

    // Pide el correo de verificacion. Guard de re-entrada: ignora toques mientras hay uno en vuelo.
    fun requestVerification() {
        if (action.value.isSending) return
        action.update { it.copy(isSending = true, errorRes = null) }
        viewModelScope.launch {
            runCatching { requestEmailVerification.invoke() }
                .onSuccess { action.update { it.copy(isSending = false, sent = true) } }
                .onFailure { throwable ->
                    // La cancelacion (VM destruido con el envio en vuelo) NO es un error de UI: se
                    // re-lanza ANTES de tocar el estado para que la corrutina termine como cancelada
                    // (toErrorMessageRes tambien la re-lanzaria, pero dentro del update y dejaria el
                    // estado a medias).
                    if (throwable is CancellationException) throw throwable
                    action.update { it.copy(isSending = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
    }

    // Refresca el usuario desde el backend (GET /auth/me) al volver a primer plano. Silencioso: un
    // fallo de red deja el valor cacheado intacto (no es una accion del usuario, no se avisa). Si el
    // correo ya se confirmo en el navegador, emailVerified pasa a true y el aviso desaparece solo.
    fun refresh() {
        // El feedback transitorio (enviado/error) no debe sobrevivir a la navegacion: al reanudar se
        // limpia para no mostrar un "enviado" o un error de hace rato (se conserva isSending por si un
        // envio sigue en vuelo). Se cancela un refresh anterior aun en vuelo para que gane el /auth/me
        // mas reciente (evita que una respuesta lenta y vieja pise a una fresca).
        action.update { it.copy(sent = false, errorRes = null) }
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            runCatching { refreshCurrentUser.invoke() }
        }
    }

    fun errorShown() {
        action.update { it.copy(errorRes = null) }
    }

    private data class ActionState(
        val isSending: Boolean = false,
        val sent: Boolean = false,
        @StringRes val errorRes: Int? = null,
    )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
