package com.eddndev.purpura.ui.addevent

import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentAddEventBinding
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder

// Mapeo enum -> chip para precargar el formulario en modo edicion (inverso de selected*() del
// Fragment). Vive aparte para mantener AddEventFragment modular y bajo el limite de tamano.

internal fun FragmentAddEventBinding.checkType(type: EventType) {
    typeChipGroup.check(
        when (type) {
            EventType.cita -> R.id.chipAddTypeCita
            EventType.junta -> R.id.chipAddTypeJunta
            EventType.entrega_proyecto -> R.id.chipAddTypeEntrega
            EventType.examen -> R.id.chipAddTypeExamen
            EventType.otros -> R.id.chipAddTypeOtros
        },
    )
}

internal fun FragmentAddEventBinding.checkStatus(status: EventStatus) {
    statusChipGroup.check(
        when (status) {
            EventStatus.pendiente -> R.id.chipAddStatusPendiente
            EventStatus.realizado -> R.id.chipAddStatusRealizado
            EventStatus.aplazado -> R.id.chipAddStatusAplazado
        },
    )
}

internal fun FragmentAddEventBinding.checkReminder(reminder: Reminder) {
    reminderChipGroup.check(
        when (reminder) {
            Reminder.none -> R.id.chipAddReminderNone
            Reminder.at_time -> R.id.chipAddReminderAtTime
            Reminder.ten_minutes_before -> R.id.chipAddReminderTen
            Reminder.one_day_before -> R.id.chipAddReminderDay
        },
    )
}
