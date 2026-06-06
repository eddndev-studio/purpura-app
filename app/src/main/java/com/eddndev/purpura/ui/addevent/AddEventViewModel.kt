package com.eddndev.purpura.ui.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventPatch
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.usecase.add.AddEventUseCase
import com.eddndev.purpura.domain.usecase.edit.UpdateEventUseCase
import com.eddndev.purpura.domain.usecase.query.GetEventUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// Anadir / Editar Evento (REQ-ADD-001..009, REQ-QUERY-011/012). En ALTA valida lo obvio, construye el
// NewEventDraft y delega en AddEventUseCase. En EDICION (mismo formulario reutilizado desde el Detalle)
// carga el evento por id, lo vuelca al formulario y al guardar construye un EventPatch -> UpdateEvent.
// La ubicacion se guarda como etiqueta (lat/lng en 0.0) salvo que el usuario elija punto en el mapa.
@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val addEvent: AddEventUseCase,
    private val getEvent: GetEventUseCase,
    private val updateEvent: UpdateEventUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEventUiState())
    val uiState: StateFlow<AddEventUiState> = _uiState.asStateFlow()

    // Evento original en edicion: es la fuente para preservar lo que el formulario no recaptura
    // (contact.ref y las coordenadas si no se reabrio el mapa) al construir el EventPatch.
    private var original: Event? = null
    private var editingId: String? = null

    // Entra en modo edicion y carga el evento por id. Idempotente: el Fragment llama en cada
    // onViewCreated, pero solo el primer id (o un reintento tras fallo) dispara la red.
    fun startEditing(id: String) {
        if (editingId == id) return
        editingId = id
        _uiState.update { it.copy(editing = true, isLoadingEvent = true, loadFailed = false, errorRes = null) }
        viewModelScope.launch {
            runCatching { getEvent(id) }
                .onSuccess { event ->
                    original = event
                    _uiState.update { it.copy(isLoadingEvent = false, loadFailed = false, prefill = event) }
                }
                .onFailure { throwable ->
                    // Reabre la puerta a reintentar; sin evento, Guardar queda bloqueado (loadFailed).
                    editingId = null
                    _uiState.update {
                        it.copy(isLoadingEvent = false, loadFailed = true, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    // El Fragment consume el prefill una sola vez (rellena los widgets) y lo limpia.
    fun prefillHandled() {
        _uiState.update { it.copy(prefill = null) }
    }

    fun submit(input: AddEventInput) {
        val state = _uiState.value
        if (state.isSubmitting || state.isLoadingEvent) return
        // En edicion sin evento cargado (carga fallida) NO se hace nada: jamas caer en la ruta de
        // alta, que crearia un evento nuevo a partir del formulario.
        if (state.editing && original == null) return

        val descriptionError = if (input.description.isBlank()) R.string.add_event_error_description else null
        val contactError = if (input.contactName.isBlank()) R.string.add_event_error_contact else null
        val dateTimeError = if (input.date == null || input.time == null) R.string.add_event_error_datetime else null
        if (descriptionError != null || contactError != null || dateTimeError != null) {
            _uiState.update {
                it.copy(
                    descriptionError = descriptionError,
                    contactError = contactError,
                    dateTimeError = dateTimeError,
                )
            }
            return
        }

        // La fecha y hora elegidas se interpretan en la zona del dispositivo (no UTC): el mismo borde
        // de zona que corregimos en el slice de Inicio. date/time ya quedaron no-nulos arriba.
        val startsAt = LocalDateTime.of(input.date, input.time)
            .atZone(ZoneId.systemDefault())
            .toInstant()

        _uiState.update {
            it.copy(
                isSubmitting = true,
                errorRes = null,
                descriptionError = null,
                contactError = null,
                dateTimeError = null,
            )
        }
        viewModelScope.launch {
            runCatching {
                val current = original
                if (current != null) {
                    updateEvent(current.id, buildPatch(current, input, startsAt))
                } else {
                    addEvent(draft = buildDraft(input, startsAt), chosenStatus = input.status)
                }
            }
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSubmitting = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
    }

    private fun buildDraft(input: AddEventInput, startsAt: Instant): NewEventDraft = NewEventDraft(
        type = input.type,
        contact = Contact(name = input.contactName.trim()),
        location = Location(
            lat = input.lat ?: 0.0,
            lng = input.lng ?: 0.0,
            label = input.placeLabel.trim().ifBlank { null },
        ),
        description = input.description.trim(),
        startsAt = startsAt,
        reminder = input.reminder,
    )

    // El parche lleva todos los campos editables del formulario, pero PRESERVA lo que este no captura:
    // contact.ref (no se edita) y, si no llegaron coordenadas nuevas, las del evento original (el mapa
    // no se reabrio). El estatus NO va aqui: en edicion el Detalle es su dueno. La etiqueta SI viene
    // del campo (en blanco => null, que el backend interpreta como limpiarla).
    private fun buildPatch(original: Event, input: AddEventInput, startsAt: Instant): EventPatch {
        val hasNewCoords = input.lat != null && input.lng != null
        val location = Location(
            lat = if (hasNewCoords) input.lat!! else original.location.lat,
            lng = if (hasNewCoords) input.lng!! else original.location.lng,
            label = input.placeLabel.trim().ifBlank { null },
        )
        return EventPatch(
            type = input.type,
            contact = Contact(name = input.contactName.trim(), ref = original.contact.ref),
            location = location,
            description = input.description.trim(),
            startsAt = startsAt,
            reminder = input.reminder,
        )
    }

    // Limpia los errores por campo cuando el usuario edita o vuelve a elegir fecha/hora.
    fun clearFieldErrors() {
        _uiState.update { it.copy(descriptionError = null, contactError = null, dateTimeError = null) }
    }

    // El aviso general es de un solo uso: el Fragment lo limpia tras mostrar el snackbar.
    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }
}
