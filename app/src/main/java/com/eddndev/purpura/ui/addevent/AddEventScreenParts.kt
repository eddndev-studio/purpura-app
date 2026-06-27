package com.eddndev.purpura.ui.addevent

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.LoadingButton
import com.eddndev.purpura.ui.compose.MapCard
import com.eddndev.purpura.ui.compose.PurpuraFilterChip
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Secciones privadas del formulario de Anadir/Editar (AddEventScreen). Viven aparte para mantener la
// pantalla bajo el limite de tamano y dejar el flujo principal legible. Cada seccion es un bloque
// SectionHeader + control, sin estado propio (lo posee la pantalla). El SectionHeader es el UNICO
// nombre de cada campo: por eso los OutlinedTextField ya no llevan label flotante (era redundante).

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
        // Area multilinea (3-5 lineas) con forma media (16dp), no pill: el texto largo respira mejor
        // en una caja de esquinas suaves. supportingText persistente reserva el hueco para que el
        // layout no salte cuando aparece/desaparece el error de validacion.
        OutlinedTextField(
            value = description,
            onValueChange = onChange,
            isError = error != null,
            supportingText = { Text(error?.let { stringResource(it) } ?: " ") },
            minLines = 3,
            maxLines = 5,
            shape = MaterialTheme.shapes.medium,
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
        // Solo lectura: se elige del telefono (REQ-ADD-002). El hint vive en el placeholder; el
        // supportingText queda para el error o la confirmacion de vinculo (no se duplica el hint).
        OutlinedTextField(
            value = contactName,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(stringResource(R.string.add_event_contact_helper)) },
            isError = error != null,
            supportingText = {
                when {
                    error != null -> Text(stringResource(error))
                    contactRef != null -> Text(stringResource(R.string.add_event_contact_linked))
                    else -> Text(" ")
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
        // Sin label flotante (lo nombra el SectionHeader). supportingText persistente: guia al usuario
        // y reserva el hueco para que el MapCard al aparecer no empuje el layout de golpe.
        OutlinedTextField(
            value = place,
            onValueChange = onPlaceChange,
            supportingText = { Text(stringResource(R.string.add_event_place_helper)) },
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
        // Confirmacion visual de la ubicacion elegida: MapCard a la altura por defecto (Spacing.mapCard).
        // Para re-elegir se usa el icono del campo; sin onOpenExternal para no mostrar "Abrir en Maps".
        AnimatedVisibility(visible = hasCoordinates) {
            if (lat != null && lng != null) {
                Column {
                    Spacer(Modifier.height(Spacing.md))
                    MapCard(
                        lat = lat,
                        lng = lng,
                        label = place.takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.add_event_location_selected),
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
            OutlinedButton(onClick = onPickDate, shape = Pill, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                // Una sola linea sin envoltura: la fecha formateada no se debe partir dentro del boton.
                Text(
                    text = date?.let { DATE_FMT.format(it) } ?: stringResource(R.string.add_event_pick_date),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            OutlinedButton(onClick = onPickTime, shape = Pill, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = time?.let { TIME_FMT.format(it) } ?: stringResource(R.string.add_event_pick_time),
                    maxLines = 1,
                    softWrap = false,
                )
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

/**
 * Seccion generica de chips de seleccion unica (Tipo / Estatus / Recordatorio). Usa la pieza de marca
 * [PurpuraFilterChip] (capsula + check al seleccionar) en lugar del FilterChip crudo de M3, asi las
 * tres secciones comparten el mismo lenguaje visual sin repetir el bloque.
 */
@Composable
internal fun <T> ChipSection(
    @StringRes titleRes: Int,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column {
        SectionHeader(stringResource(titleRes))
        ChipRow {
            options.forEach { option ->
                PurpuraFilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = label(option),
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

/**
 * CTA fija de guardado, anclada en el slot bottomBar del PurpuraScreen (fuera del scroll). Un divisor
 * de 1dp + Surface con leve elevacion tonal la despegan del formulario; imePadding la levanta sobre el
 * teclado. LoadingButton da el progreso en linea sin mover el layout.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SaveBar(
    editing: Boolean,
    busy: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Column(Modifier.imePadding()) {
            HorizontalDivider(thickness = 1.dp)
            LoadingButton(
                onClick = onClick,
                text = stringResource(
                    if (editing) R.string.add_event_update_action else R.string.add_event_save,
                ),
                isLoading = busy,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            )
        }
    }
}
