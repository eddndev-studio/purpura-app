package com.eddndev.purpura.ui.addevent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.NewEventDraft
import com.eddndev.purpura.domain.usecase.add.AddEventUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

// Anadir Evento (REQ-ADD-001..009). Valida localmente lo obvio (descripcion, contacto, fecha+hora),
// construye el NewEventDraft y delega en AddEventUseCase (crea con status=pendiente y, si el estatus
// elegido es otro, lo aplica en un segundo PATCH; ademas programa el recordatorio). La ubicacion se
// guarda como etiqueta (lat/lng en 0.0) hasta tener el selector de mapa.
@HiltViewModel
class AddEventViewModel @Inject constructor(
    private val addEvent: AddEventUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEventUiState())
    val uiState: StateFlow<AddEventUiState> = _uiState.asStateFlow()

    fun submit(input: AddEventInput) {
        if (_uiState.value.isSubmitting) return

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
                addEvent(
                    draft = NewEventDraft(
                        type = input.type,
                        contact = Contact(name = input.contactName.trim()),
                        location = Location(lat = 0.0, lng = 0.0, label = input.placeLabel.trim().ifBlank { null }),
                        description = input.description.trim(),
                        startsAt = startsAt,
                        reminder = input.reminder,
                    ),
                    chosenStatus = input.status,
                )
            }
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, saved = true) } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSubmitting = false, errorRes = throwable.toErrorMessageRes()) }
                }
        }
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
