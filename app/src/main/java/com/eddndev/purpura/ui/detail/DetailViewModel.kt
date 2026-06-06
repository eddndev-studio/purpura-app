package com.eddndev.purpura.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.usecase.edit.ChangeEventStatusUseCase
import com.eddndev.purpura.domain.usecase.edit.DeleteEventUseCase
import com.eddndev.purpura.domain.usecase.query.GetEventUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Detalle y edicion de un evento (REQ-QUERY-007..013). Carga por id (network-first via getById),
// permite cambiar el estatus y eliminar. El id llega por argumento de navegacion y se entrega con
// load(): asi el VM es testeable sin SavedStateHandle. Errores -> @StringRes (un solo uso), sin
// borrar el evento ya cargado.
//
// Alcance v1: ver + cambiar estatus + eliminar. La edicion de campos (formulario completo) llega
// con AddEvent (#8) para reutilizar el mismo formulario en lugar de duplicarlo.
@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getEvent: GetEventUseCase,
    private val changeEventStatus: ChangeEventStatusUseCase,
    private val deleteEvent: DeleteEventUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState.Initial)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private var eventId: String? = null
    private var loadedId: String? = null

    // Idempotente por id mientras la carga va bien o esta en vuelo: el Fragment llama load() en
    // cada onViewCreated, pero solo la primera vez (o ante un id distinto) dispara la red. Un fallo
    // resetea loadedId para que un reintento (retry()) o un nuevo onViewCreated vuelva a intentar.
    // TODO(#8): getById es network-first; abrir el Detalle sin conexion falla aunque el evento ya
    // este en cache (Inicio/Consultar lo muestran desde Room). A futuro: sembrar el Detalle desde
    // EventDao.findById y refrescar en segundo plano para cubrir la politica offline tambien aqui.
    fun load(id: String) {
        if (loadedId == id) return
        loadedId = id
        eventId = id
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false, errorRes = null) }
            runCatching { getEvent(id) }
                .onSuccess { event ->
                    _uiState.update { it.copy(event = event, isLoading = false, loadFailed = false) }
                }
                .onFailure { throwable ->
                    // Propaga la cancelacion (no es un fallo real); cualquier otro error desbloquea
                    // el reintento y muestra el estado de error en lugar de una pantalla en blanco.
                    val messageRes = throwable.toErrorMessageRes()
                    loadedId = null
                    _uiState.update { it.copy(isLoading = false, loadFailed = true, errorRes = messageRes) }
                }
        }
    }

    // Reintenta la carga tras un fallo (boton "Reintentar" del estado de error).
    fun retry() {
        val id = eventId ?: return
        loadedId = null
        load(id)
    }

    // Fuerza una recarga aunque el id no cambie: lo invoca el Detalle al volver del formulario de
    // edicion (FragmentResult), porque load() es idempotente por id y de otro modo mostraria el
    // evento sin los cambios recien guardados.
    fun refresh() {
        val id = eventId ?: return
        loadedId = null
        load(id)
    }

    fun changeStatus(status: EventStatus) {
        val id = eventId ?: return
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorRes = null) }
            runCatching { changeEventStatus(id, status) }
                .onSuccess { event -> _uiState.update { it.copy(event = event, isWorking = false) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isWorking = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
    }

    fun delete() {
        val id = eventId ?: return
        if (_uiState.value.isWorking) return
        viewModelScope.launch {
            _uiState.update { it.copy(isWorking = true, errorRes = null) }
            runCatching { deleteEvent(id) }
                .onSuccess { _uiState.update { it.copy(isWorking = false, deleted = true) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isWorking = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }
}
