package com.eddndev.purpura.ui.addevent

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * Dialogos de fecha y hora en Compose (M3) para el formulario de evento. Reemplazan a
 * MaterialDatePicker/MaterialTimePicker (DialogFragment): asi la fecha/hora viven como estado de la
 * pantalla y sobreviven a la rotacion sin plomeria en el Fragment. La fecha se interpreta en UTC al
 * elegir (igual que el formulario original) y se reinterpreta en la zona local al guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurpuraDatePickerDialog(
    initial: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    // Confirmar deshabilitado hasta que haya una fecha elegida: evita cerrar el dialogo sin seleccion.
    val hasSelection = datePickerState.selectedDateMillis != null
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = hasSelection,
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
            ) { Text(stringResource(R.string.action_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        // title del propio DatePicker: encabezado coherente con el resto del formulario.
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = stringResource(R.string.add_event_date_picker_title),
                    modifier = Modifier.padding(
                        start = Spacing.xl,
                        end = Spacing.md,
                        top = Spacing.lg,
                    ),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurpuraTimePickerDialog(
    initial: LocalTime?,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val timeState = rememberTimePickerState(
        initialHour = initial?.hour ?: DEFAULT_HOUR,
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onConfirm(LocalTime.of(timeState.hour, timeState.minute))
                onDismiss()
            }) { Text(stringResource(R.string.action_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        title = { Text(stringResource(R.string.add_event_time_picker_title)) },
        text = { TimePicker(state = timeState) },
    )
}

private const val DEFAULT_HOUR = 12
