package com.eddndev.purpura.ui.addevent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.MapCard
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Secciones privadas del formulario de Anadir/Editar (AddEventScreen). Viven aparte para mantener la
// pantalla bajo el limite de tamano y dejar el flujo principal legible. Cada seccion es un bloque
// SectionHeader + control, sin estado propio (lo posee la pantalla).

private val LOCALE = Locale("es", "MX")
private val DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE)
private val TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", LOCALE)

@Composable
internal fun DescriptionSection(
    description: String,
    error: Int?,
    onChange: (String) -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.add_event_description_label))
        OutlinedTextField(
            value = description,
            onValueChange = onChange,
            label = { Text(stringResource(R.string.add_event_description_label)) },
            isError = error != null,
            supportingText = error?.let { { Text(stringResource(it)) } },
            shape = Pill,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun ContactSection(
    contactName: String,
    contactRef: String?,
    error: Int?,
    onPickContact: () -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.add_event_contact_label))
        // Solo lectura: se elige del telefono (REQ-ADD-002). El icono lanza el selector.
        OutlinedTextField(
            value = contactName,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.add_event_contact_label)) },
            isError = error != null,
            supportingText = {
                when {
                    error != null -> Text(stringResource(error))
                    contactRef != null -> Text(stringResource(R.string.add_event_contact_linked))
                    else -> Text(stringResource(R.string.add_event_contact_helper))
                }
            },
            trailingIcon = {
                IconButton(onClick = onPickContact) {
                    Icon(
                        Icons.Outlined.Person,
                        contentDescription = stringResource(R.string.add_event_pick_contact),
                    )
                }
            },
            shape = Pill,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun PlaceSection(
    place: String,
    lat: Double?,
    lng: Double?,
    onPlaceChange: (String) -> Unit,
    onPickLocation: () -> Unit,
) {
    val hasCoordinates = lat != null && lng != null
    Column {
        SectionHeader(stringResource(R.string.add_event_place_label))
        OutlinedTextField(
            value = place,
            onValueChange = onPlaceChange,
            label = { Text(stringResource(R.string.add_event_place_label)) },
            trailingIcon = {
                IconButton(onClick = onPickLocation) {
                    Icon(
                        Icons.Outlined.Map,
                        contentDescription = stringResource(R.string.add_event_pick_location),
                    )
                }
            },
            shape = Pill,
            modifier = Modifier.fillMaxWidth(),
        )
        // Confirmacion visual de la ubicacion elegida: MapCard pulido en lugar de un texto suelto.
        AnimatedVisibility(visible = hasCoordinates) {
            if (lat != null && lng != null) {
                Column {
                    Spacer(Modifier.height(Spacing.md))
                    // Para re-elegir se usa el icono del campo; no se pasa onOpenExternal para no
                    // mostrar el boton "Abrir en Maps" aqui (re-abriria el selector, no Maps).
                    MapCard(
                        lat = lat,
                        lng = lng,
                        label = place.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.add_event_location_selected),
                        height = 160.dp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DateTimeSection(
    date: LocalDate?,
    time: LocalTime?,
    error: Int?,
    onPickDate: () -> Unit,
    onPickTime: () -> Unit,
) {
    Column {
        SectionHeader(stringResource(R.string.add_event_datetime_label))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            OutlinedButton(
                onClick = onPickDate,
                shape = Pill,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(date?.let { DATE_FMT.format(it) } ?: stringResource(R.string.add_event_pick_date))
            }
            OutlinedButton(
                onClick = onPickTime,
                shape = Pill,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(time?.let { TIME_FMT.format(it) } ?: stringResource(R.string.add_event_pick_time))
            }
        }
        AnimatedVisibility(visible = error != null) {
            Text(
                text = error?.let { stringResource(it) }.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Spacing.xs, start = Spacing.xs),
            )
        }
    }
}

@Composable
internal fun SaveButton(
    editing: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Pill,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Crossfade entre el spinner y la etiqueta para una transicion suave al guardar.
        Crossfade(targetState = busy, label = "save-button") { loading ->
            if (loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text(
                    stringResource(
                        if (editing) R.string.add_event_update_action else R.string.add_event_save,
                    ),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipRow(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        content = content,
    )
}
