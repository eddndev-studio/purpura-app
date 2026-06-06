package com.eddndev.purpura.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.eddndev.purpura.R

// Canal de notificaciones de recordatorios (REQ-NOTIF-001). Se crea una vez al iniciar la app
// (PurpuraApplication). Con minSdk 26 el canal es siempre obligatorio. El id es estable: usarlo
// tanto al crear el canal como al publicar la notificacion.
object ReminderNotifications {

    const val CHANNEL_ID = "purpura_reminders"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }

    // Id estable de notificacion por evento: permite reemplazar/cancelar la del mismo evento.
    fun notificationId(eventId: String): Int = eventId.hashCode()
}
