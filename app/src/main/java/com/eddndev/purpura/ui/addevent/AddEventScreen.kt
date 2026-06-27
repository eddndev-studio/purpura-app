package com.eddndev.purpura.ui.addevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.eddndev.purpura.ui.compose.ErrorState
import com.eddndev.purpura.ui.compose.LoadingState
import com.eddndev.purpura.ui.compose.PurpuraScreen
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

// Fase visible del formulario. Solo importa en EDICION (carga del evento): en ALTA cae siempre en Form.
// El CTA de guardar solo se muestra en Form (no sobre el spinner ni sobre el error de carga).
private enum class AddEventPhase { Loading, Error, Form }

/**
 * Anadir / Editar Evento (REQ-ADD-001..009) en Compose. Envuelve todo en [PurpuraScreen] con flecha
 * atras y titulo segun el modo. La pantalla posee TODO el estado del formulario (rememberSaveable) y
 * los pickers de fecha/hora; el Fragment solo entrega los resultados de los selectores externos
 * (contacto del sistema, mapa) por [externalPicks] y conserva permisos y navegacion. Al guardar
 * construye [AddEventInput] y delega en [onSubmit]; la validacion vive en el VM. En edicion, las tres
 * fases (cargando/fallo/formulario) se cruzan con Crossfade. Las secciones del formulario y la CTA
 * viven en AddEventScreenParts.kt para mantener este archivo bajo el limite de tamano.
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
        val loadFailed = state.loadFailed
        LaunchedEffect(messageRes, text, loadFailed) {
            // En fallo de carga el ErrorState (Crossfade) ya ofrece reintentar; el snackbar solo cubre
            // los errores de guardado, para no duplicar la accion de reintento.
            if (!loadFailed) snackbarHostState.showSnackbar(text)
            onErrorShown()
        }
    }

    val busy = state.isSubmitting || state.isLoadingEvent
    val phase = when {
        state.isLoadingEvent -> AddEventPhase.Loading
        state.loadFailed -> AddEventPhase.Error
        else -> AddEventPhase.Form
    }

    PurpuraScreen(
        title = stringResource(
            if (state.editing) R.string.add_event_edit_title else R.string.title_add_event,
        ),
        modifier = modifier,
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // CTA fija al fondo solo cuando hay formulario que guardar (no sobre carga/error).
        bottomBar = if (phase == AddEventPhase.Form) {
            {
                SaveBar(
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
        } else {
            null
        },
    ) { innerPadding ->
        Crossfade(
            targetState = phase,
            animationSpec = tween(200),
            label = "addEventPhase",
            modifier = Modifier.padding(innerPadding).fillMaxSize(),
        ) { current ->
            when (current) {
                AddEventPhase.Loading -> LoadingState()

                AddEventPhase.Error -> ErrorState(
                    icon = Icons.Outlined.ErrorOutline,
                    message = stringResource(R.string.detail_load_error_title),
                    retryLabel = stringResource(R.string.detail_retry),
                    onRetry = onRetryLoad,
                )

                AddEventPhase.Form -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        // Sin imePadding aqui: el SaveBar (bottomBar) ya libera el teclado y el Scaffold
                        // propaga ese alto al innerPadding del contenido. Ponerlo aqui lo contaria doble.
                        .padding(horizontal = Spacing.screenH)
                        .padding(top = Spacing.sm, bottom = Spacing.xl)
                        // Suaviza el reflujo al aparecer/desaparecer el MapCard o la seccion de estatus.
                        .animateContentSize(animationSpec = tween(200)),
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

                    ChipSection(
                        titleRes = R.string.add_event_type_label,
                        options = EventType.entries,
                        selected = type,
                        label = { stringResource(EventDisplay.typeLabel(it)) },
                        onSelect = { type = it },
                    )

                    DateTimeSection(
                        date = date,
                        time = time,
                        error = state.dateTimeError,
                        onPickDate = { showDatePicker = true },
                        onPickTime = { showTimePicker = true },
                    )

                    // Estatus: solo en alta; en edicion el Detalle es el dueno del estatus.
                    AnimatedVisibility(visible = !state.editing) {
                        ChipSection(
                            titleRes = R.string.add_event_status_label,
                            options = EventStatus.entries,
                            selected = status,
                            label = { stringResource(EventDisplay.statusLabel(it)) },
                            onSelect = { status = it },
                        )
                    }

                    ChipSection(
                        titleRes = R.string.add_event_reminder_label,
                        options = Reminder.entries,
                        selected = reminder,
                        label = { stringResource(EventDisplay.reminderLabel(it)) },
                        onSelect = { reminder = it },
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
