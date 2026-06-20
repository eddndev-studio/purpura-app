package com.eddndev.purpura.ui.addevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.theme.Pill
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Resultado de un selector externo que vive en el Fragment (contacto del sistema o selector de mapa). */
sealed interface ExternalPick {
    data class Contact(val name: String, val ref: String?) : ExternalPick
    data class Location(val lat: Double, val lng: Double, val label: String?) : ExternalPick
}

private val LOCALE = Locale("es", "MX")
private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", LOCALE)

/**
 * Anadir / Editar Evento (REQ-ADD-001..009) en Compose. La pantalla posee TODO el estado del
 * formulario (rememberSaveable) y los pickers de fecha/hora; el Fragment solo entrega los resultados
 * de los selectores externos (contacto del sistema, mapa) por [externalPicks] y conserva permisos y
 * navegacion. Al guardar construye [AddEventInput] y delega en [onSubmit]; la validacion vive en el VM.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    state: AddEventUiState,
    externalPicks: Flow<ExternalPick>,
    onPickContact: () -> Unit,
    onPickLocation: (Double?, Double?) -> Unit,
    onSubmit: (AddEventInput) -> Unit,
    onClearFieldErrors: () -> Unit,
    onPrefillHandled: () -> Unit,
    onSaved: () -> Unit,
    onRetryLoad: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var description by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var contactName by rememberSaveable { mutableStateOf("") }
    var contactRef by rememberSaveable { mutableStateOf<String?>(null) }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lng by rememberSaveable { mutableStateOf<Double?>(null) }
    var type by rememberSaveable { mutableStateOf(EventType.cita) }
    var status by rememberSaveable { mutableStateOf(EventStatus.pendiente) }
    var reminder by rememberSaveable { mutableStateOf(Reminder.none) }
    var date by rememberSaveable { mutableStateOf<LocalDate?>(null) }
    var time by rememberSaveable { mutableStateOf<LocalTime?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Resultados de los selectores externos del Fragment (contacto / mapa).
    LaunchedEffect(Unit) {
        externalPicks.collect { pick ->
            when (pick) {
                is ExternalPick.Contact -> {
                    contactName = pick.name
                    contactRef = pick.ref
                    onClearFieldErrors()
                }
                is ExternalPick.Location -> {
                    lat = pick.lat
                    lng = pick.lng
                    if (place.isBlank() && !pick.label.isNullOrBlank()) place = pick.label
                }
            }
        }
    }

    // Prefill de edicion (payload de un solo uso): vuelca el evento cargado en el formulario.
    LaunchedEffect(state.prefill) {
        val event = state.prefill ?: return@LaunchedEffect
        description = event.description
        contactName = event.contact.name
        contactRef = event.contact.ref
        place = event.location.label.orEmpty()
        type = event.type
        status = event.status
        reminder = event.reminder
        val zoned = event.startsAt.atZone(ZoneId.systemDefault())
        date = zoned.toLocalDate()
        time = zoned.toLocalTime()
        if (event.location.lat != 0.0 || event.location.lng != 0.0) {
            lat = event.location.lat
            lng = event.location.lng
        }
        onPrefillHandled()
    }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    state.errorRes?.let { messageRes ->
        val text = stringResource(messageRes)
        val retryLabel = stringResource(R.string.detail_retry)
        val loadFailed = state.loadFailed
        LaunchedEffect(messageRes, text) {
            if (loadFailed) {
                val result = snackbarHostState.showSnackbar(text, actionLabel = retryLabel)
                if (result == SnackbarResult.ActionPerformed) onRetryLoad()
            } else {
                snackbarHostState.showSnackbar(text)
            }
            onErrorShown()
        }
    }

    val busy = state.isSubmitting || state.isLoadingEvent

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            OutlinedTextField(
                value = description,
                onValueChange = { description = it; onClearFieldErrors() },
                label = { Text(stringResource(R.string.add_event_description_label)) },
                isError = state.descriptionError != null,
                supportingText = state.descriptionError?.let { { Text(stringResource(it)) } },
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            // Contacto: solo lectura, se elige del telefono (REQ-ADD-002). El icono lanza el selector.
            OutlinedTextField(
                value = contactName,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.add_event_contact_label)) },
                isError = state.contactError != null,
                supportingText = {
                    when {
                        state.contactError != null -> Text(stringResource(state.contactError))
                        contactRef != null -> Text(stringResource(R.string.add_event_contact_linked))
                        else -> Text(stringResource(R.string.add_event_contact_helper))
                    }
                },
                trailingIcon = {
                    IconButton(onClick = onPickContact) {
                        Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.add_event_pick_contact))
                    }
                },
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = place,
                onValueChange = { place = it },
                label = { Text(stringResource(R.string.add_event_place_label)) },
                trailingIcon = {
                    IconButton(onClick = { onPickLocation(lat, lng) }) {
                        Icon(Icons.Outlined.Place, contentDescription = stringResource(R.string.add_event_pick_location))
                    }
                },
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            )
            AnimatedVisibility(visible = lat != null && lng != null) {
                Text(
                    text = stringResource(R.string.add_event_location_selected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))

            // Tipo
            SectionLabel(stringResource(R.string.add_event_type_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EventType.entries.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option },
                        label = { Text(stringResource(EventDisplay.typeLabel(option))) },
                        shape = Pill,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Fecha y hora
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = Pill,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(date?.let { DATE_FMT.format(it) } ?: stringResource(R.string.add_event_pick_date))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    shape = Pill,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(time?.let { TIME_FMT.format(it) } ?: stringResource(R.string.add_event_pick_time))
                }
            }
            AnimatedVisibility(visible = state.dateTimeError != null) {
                Text(
                    text = state.dateTimeError?.let { stringResource(it) }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))

            // Estatus (oculto en edicion: el Detalle es el dueno del estatus)
            AnimatedVisibility(visible = !state.editing) {
                Column {
                    SectionLabel(stringResource(R.string.add_event_status_label))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        EventStatus.entries.forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(stringResource(EventDisplay.statusLabel(option))) },
                                shape = Pill,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            // Recordatorio
            SectionLabel(stringResource(R.string.add_event_reminder_label))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Reminder.entries.forEach { option ->
                    FilterChip(
                        selected = reminder == option,
                        onClick = { reminder = option },
                        label = { Text(stringResource(EventDisplay.reminderLabel(option))) },
                        shape = Pill,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSubmit(
                        AddEventInput(
                            description = description,
                            contactName = contactName,
                            placeLabel = place,
                            type = type,
                            status = status,
                            reminder = reminder,
                            date = date,
                            time = time,
                            lat = lat,
                            lng = lng,
                            contactRef = contactRef,
                        ),
                    )
                },
                enabled = !busy && !state.loadFailed,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (busy) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        stringResource(
                            if (state.editing) R.string.add_event_update_action else R.string.add_event_save,
                        ),
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        PurpuraDatePickerDialog(
            initial = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it; onClearFieldErrors() },
        )
    }
    if (showTimePicker) {
        PurpuraTimePickerDialog(
            initial = time,
            onDismiss = { showTimePicker = false },
            onConfirm = { time = it; onClearFieldErrors() },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
