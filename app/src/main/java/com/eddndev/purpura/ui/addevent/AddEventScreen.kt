package com.eddndev.purpura.ui.addevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
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
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.PurpuraScreen
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Resultado de un selector externo que vive en el Fragment (contacto del sistema o selector de mapa). */
sealed interface ExternalPick {
    data class Contact(val name: String, val ref: String?) : ExternalPick
    data class Location(val lat: Double, val lng: Double, val label: String?) : ExternalPick
}

/**
 * Anadir / Editar Evento (REQ-ADD-001..009) en Compose. Envuelve todo en [PurpuraScreen] con flecha
 * atras y titulo segun el modo. La pantalla posee TODO el estado del formulario (rememberSaveable) y
 * los pickers de fecha/hora; el Fragment solo entrega los resultados de los selectores externos
 * (contacto del sistema, mapa) por [externalPicks] y conserva permisos y navegacion. Al guardar
 * construye [AddEventInput] y delega en [onSubmit]; la validacion vive en el VM. Las secciones del
 * formulario viven en AddEventScreenParts.kt para mantener este archivo bajo el limite de tamano.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEventScreen(
    state: AddEventUiState,
    externalPicks: Flow<ExternalPick>,
    // onBack: flecha atras del TopAppBar (spec: Anadir/Editar -> navigateUp). Default no-op para no
    // romper a los llamadores existentes; el Fragment lo cablea con findNavController().navigateUp().
    onBack: () -> Unit = {},
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

    PurpuraScreen(
        title = stringResource(
            if (state.editing) R.string.add_event_edit_title else R.string.title_add_event,
        ),
        modifier = modifier,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = Spacing.screenH)
                .padding(top = Spacing.sm, bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.section),
        ) {
            DescriptionSection(
                description = description,
                error = state.descriptionError,
                onChange = { description = it; onClearFieldErrors() },
            )

            ContactSection(
                contactName = contactName,
                contactRef = contactRef,
                error = state.contactError,
                onPickContact = onPickContact,
            )

            PlaceSection(
                place = place,
                lat = lat,
                lng = lng,
                onPlaceChange = { place = it },
                onPickLocation = { onPickLocation(lat, lng) },
            )

            // Tipo: chips reflow en FlowRow (REQ-ADD-003).
            Column {
                SectionHeader(stringResource(R.string.add_event_type_label))
                ChipRow {
                    EventType.entries.forEach { option ->
                        FilterChip(
                            selected = type == option,
                            onClick = { type = option },
                            label = { Text(stringResource(EventDisplay.typeLabel(option))) },
                            shape = Pill,
                        )
                    }
                }
            }

            DateTimeSection(
                date = date,
                time = time,
                error = state.dateTimeError,
                onPickDate = { showDatePicker = true },
                onPickTime = { showTimePicker = true },
            )

            // Estatus: solo en alta; en edicion el Detalle es el dueno del estatus.
            AnimatedVisibility(visible = !state.editing) {
                Column {
                    SectionHeader(stringResource(R.string.add_event_status_label))
                    ChipRow {
                        EventStatus.entries.forEach { option ->
                            FilterChip(
                                selected = status == option,
                                onClick = { status = option },
                                label = { Text(stringResource(EventDisplay.statusLabel(option))) },
                                shape = Pill,
                            )
                        }
                    }
                }
            }

            // Recordatorio
            Column {
                SectionHeader(stringResource(R.string.add_event_reminder_label))
                ChipRow {
                    Reminder.entries.forEach { option ->
                        FilterChip(
                            selected = reminder == option,
                            onClick = { reminder = option },
                            label = { Text(stringResource(EventDisplay.reminderLabel(option))) },
                            shape = Pill,
                        )
                    }
                }
            }

            SaveButton(
                editing = state.editing,
                busy = busy,
                enabled = !busy && !state.loadFailed,
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
            )
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
