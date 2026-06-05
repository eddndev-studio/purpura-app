package com.eddndev.purpura.ui.home

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// Traduce los enums del dominio (codigos ASCII) a etiquetas en espanol + colores de marca, y
// formatea el instante a hora local. Vive en la capa de UI: el ViewModel no resuelve recursos.
object EventDisplay {

    private val LOCALE = Locale("es", "MX")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    private val DATE = DateTimeFormatter.ofPattern("EEE d MMM", LOCALE)

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
}
