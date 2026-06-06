package com.eddndev.purpura.ui.common

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Traduce los enums del dominio (codigos ASCII) a etiquetas en espanol + colores de marca, y
// formatea el instante a hora local. Vive en la capa de UI (compartido por Inicio, Consultar y
// Detalle): el ViewModel no resuelve recursos.
object EventDisplay {

    private val LOCALE = Locale("es", "MX")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    private val DATE = DateTimeFormatter.ofPattern("EEE d MMM", LOCALE)
    private val FULL_DATE = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM yyyy", LOCALE)

    @StringRes
    fun typeLabel(type: EventType): Int = when (type) {
        EventType.cita -> R.string.event_type_cita
        EventType.junta -> R.string.event_type_junta
        EventType.entrega_proyecto -> R.string.event_type_entrega_proyecto
        EventType.examen -> R.string.event_type_examen
        EventType.otros -> R.string.event_type_otros
    }

    @ColorRes
    fun typeColor(type: EventType): Int = when (type) {
        EventType.cita -> R.color.event_cita
        EventType.junta -> R.color.event_junta
        EventType.entrega_proyecto -> R.color.event_entrega_proyecto
        EventType.examen -> R.color.event_examen
        EventType.otros -> R.color.event_otros
    }

    @ColorRes
    fun typeContainer(type: EventType): Int = when (type) {
        EventType.cita -> R.color.event_cita_container
        EventType.junta -> R.color.event_junta_container
        EventType.entrega_proyecto -> R.color.event_entrega_proyecto_container
        EventType.examen -> R.color.event_examen_container
        EventType.otros -> R.color.event_otros_container
    }

    @StringRes
    fun statusLabel(status: EventStatus): Int = when (status) {
        EventStatus.pendiente -> R.string.event_status_pendiente
        EventStatus.realizado -> R.string.event_status_realizado
        EventStatus.aplazado -> R.string.event_status_aplazado
    }

    @ColorRes
    fun statusColor(status: EventStatus): Int = when (status) {
        EventStatus.pendiente -> R.color.status_pendiente
        EventStatus.realizado -> R.color.status_realizado
        EventStatus.aplazado -> R.color.status_aplazado
    }

    @ColorRes
    fun statusContainer(status: EventStatus): Int = when (status) {
        EventStatus.pendiente -> R.color.status_pendiente_container
        EventStatus.realizado -> R.color.status_realizado_container
        EventStatus.aplazado -> R.color.status_aplazado_container
    }

    // "Hoy · 15:30", "Manana · 09:00" o "Vie 12 jun · 18:00" segun la zona del dispositivo.
    fun formatWhen(context: Context, startsAt: Instant): String {
        val zone = ZoneId.systemDefault()
        val dateTime = startsAt.atZone(zone)
        val eventDate = dateTime.toLocalDate()
        val today = LocalDate.now(zone)
        val day = when (eventDate) {
            today -> context.getString(R.string.day_today)
            today.plusDays(1) -> context.getString(R.string.day_tomorrow)
            else -> dateTime.format(DATE).replaceFirstChar { it.titlecase(LOCALE) }
        }
        return context.getString(R.string.home_event_when, day, dateTime.format(TIME))
    }

    // "Viernes 12 de junio 2026" para el encabezado de Detalle (fecha completa en la zona local).
    fun formatFullDate(startsAt: Instant): String =
        startsAt.atZone(ZoneId.systemDefault()).format(FULL_DATE)
            .replaceFirstChar { it.titlecase(LOCALE) }

    // "15:30" en la zona del dispositivo.
    fun formatTime(startsAt: Instant): String =
        startsAt.atZone(ZoneId.systemDefault()).format(TIME)

    @StringRes
    fun reminderLabel(reminder: Reminder): Int = when (reminder) {
        Reminder.none -> R.string.reminder_none
        Reminder.at_time -> R.string.reminder_at_time
        Reminder.ten_minutes_before -> R.string.reminder_ten_minutes_before
        Reminder.one_day_before -> R.string.reminder_one_day_before
    }
}
