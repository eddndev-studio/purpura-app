package com.eddndev.purpura.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.triggerAt
import com.eddndev.purpura.domain.repository.ReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

// Implementacion real del puerto de recordatorios (REQ-NOTIF-001) con AlarmManager. El instante de
// disparo lo da la regla pura del dominio (Reminder.triggerAt). Programa una alarma EXACTA
// (setExactAndAllowWhileIdle) para que el aviso dispare a la hora aun con el dispositivo en Doze;
// Purpura declara USE_EXACT_ALARM (API 33+, aplica por ser app de recordatorios). Si el sistema no
// concede alarmas exactas (p.ej. API 31-32 con el permiso revocado) cae a una inexacta. Al
// dispararse, ReminderReceiver publica la notificacion. cancel() retira la alarma pendiente.
//
// NOTA: en OEM agresivos (Vivo/Xiaomi/Huawei/etc.) la alarma puede no dispararse aunque sea exacta si
// la app no esta exenta de la optimizacion de bateria / autostart: eso es ajuste del dispositivo, no
// del codigo.
//
// TODO(#8): reprogramar tras reinicio (BootReceiver + ScheduleReminderUseCase). AlarmManager pierde
// las alarmas al apagar el dispositivo; no es critico para la entrega.
@Singleton
class AlarmManagerReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(event: Event) {
        val trigger = event.reminder.triggerAt(event.startsAt)
        // Sin recordatorio, o la hora de disparo ya paso: no programa nada y limpia cualquier alarma
        // previa del mismo evento (p.ej. si se cambio el recordatorio a "none" o se adelanto la fecha).
        if (trigger == null || !trigger.isAfter(Instant.now())) {
            cancel(event.id)
            return
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_ID, event.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, event.description)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode(event.id),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val triggerMillis = trigger.toEpochMilli()
        if (canScheduleExact()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    // API 31+ exige permiso para alarmas exactas; antes no se requeria. canScheduleExactAlarms()
    // refleja si el sistema lo concede (con USE_EXACT_ALARM es true en API 33+). Si no, inexacta.
    private fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    override fun cancel(eventId: String) {
        // FLAG_NO_CREATE devuelve null si no habia alarma: nada que cancelar.
        val existing = PendingIntent.getBroadcast(
            context,
            requestCode(eventId),
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
        existing?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }

    // Codigo de peticion estable por evento: identifica la alarma para reemplazarla o cancelarla. Los
    // extras del Intent no participan en la igualdad de PendingIntent, asi que cancelar sin ellos
    // sigue casando con la alarma programada.
    private fun requestCode(eventId: String): Int = eventId.hashCode()
}
