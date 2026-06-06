package com.eddndev.purpura.ui.addevent

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import java.time.LocalDate
import java.time.LocalTime

// Carga del formulario de Anadir Evento (REQ-ADD-001..009). El Fragment reune los valores de los
// widgets y los pasa al ViewModel, que valida y construye el NewEventDraft. La ubicacion se captura
// solo como etiqueta de texto por ahora; el selector de mapa (lat/lng) llega cuando se resuelva el
// billing de Maps (ver [[purpura-gcloud-setup]]).
data class AddEventInput(
    val description: String,
    val contactName: String,
    val placeLabel: String,
    val type: EventType,
    val status: EventStatus,
    val reminder: Reminder,
    val date: LocalDate?,
    val time: LocalTime?,
    // Coordenadas elegidas en el mapa (REQ-ADD-005). Null si el usuario no abrio el selector: en ese
    // caso se conserva el comportamiento previo (lat/lng en 0.0, solo etiqueta de texto).
    val lat: Double? = null,
    val lng: Double? = null,
)

// Estado observable del formulario. Los errores por campo son @StringRes (consistente con los slices
// de Consultar/Detalle: el ViewModel ya resuelve a recurso). `errorRes` es el aviso general de un
// solo uso (snackbar); `saved` dispara la navegacion de regreso a Inicio una sola vez.
data class AddEventUiState(
    val isSubmitting: Boolean = false,
    @StringRes val descriptionError: Int? = null,
    @StringRes val contactError: Int? = null,
    @StringRes val dateTimeError: Int? = null,
    @StringRes val errorRes: Int? = null,
    val saved: Boolean = false,
)
