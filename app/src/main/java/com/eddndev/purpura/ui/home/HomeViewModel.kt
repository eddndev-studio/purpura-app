package com.eddndev.purpura.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.usecase.home.GetUpcomingEventsUseCase
import com.eddndev.purpura.domain.usecase.home.RefreshUpcomingEventsUseCase
import com.eddndev.purpura.ui.common.toMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// Inicio (REQ-HOME-001). Observa el cache (Room) de la ventana [hoy, hoy+4] y lo combina con el
// estado del refresh contra la API. El cache es la fuente de verdad: un refresh fallido NO borra
// la lista; solo emite un aviso de un solo uso. `today` se calcula una vez y alimenta AMBOS casos
// de uso para que observar y sincronizar usen exactamente la misma ventana.
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getUpcomingEvents: GetUpcomingEventsUseCase,
    private val refreshUpcomingEvents: RefreshUpcomingEventsUseCase,
) : ViewModel() {

    // TODO(#8): `today` se congela al construir el VM. Si la app queda abierta cruzando la
    // medianoche, la ventana no rueda. Mitigacion futura: un Flow de fecha + flatMapLatest, o
    // refrescar la fecha en ON_RESUME. Minor: requiere la app viva toda la noche.
    private val today: LocalDate = LocalDate.now()

    private val refreshing = MutableStateFlow(true)
    private val errorRes = MutableStateFlow<Int?>(null)
    private var refreshJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        getUpcomingEvents(today),
        refreshing,
        errorRes,
    ) { events, isRefreshing, error ->
        HomeUiState(events = events, isLoading = isRefreshing, errorRes = error)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState.Initial,
    )

    init {
        refresh()
    }

    // Dispara la sincronizacion contra la API (carga inicial y pull-to-refresh). Ignora la llamada
    // si ya hay un refresh en vuelo, evitando solapes.
    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshing.value = true
            errorRes.value = null
            // NOTA: se mapea el error a @StringRes aqui para ser consistente con AuthViewModel
            // (AuthUiState.Error tambien lleva @StringRes). Revisar ambos juntos si se decide
            // mover el mapeo al Fragment (pureza de capas) para no divergir un solo slice.
            runCatching { refreshUpcomingEvents(today) }
                .onFailure { throwable ->
                    val error = throwable as? DomainError ?: DomainError.Unexpected(throwable)
                    errorRes.value = error.toMessageRes()
                }
            refreshing.value = false
        }
    }

    fun errorShown() {
        errorRes.value = null
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
