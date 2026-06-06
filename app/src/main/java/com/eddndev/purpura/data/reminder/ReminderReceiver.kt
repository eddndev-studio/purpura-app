package com.eddndev.purpura.data.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.MainActivity

// Recibe la alarma programada por AlarmManagerReminderScheduler y publica la notificacion local del
// recordatorio (REQ-NOTIF-001). Al tocarla abre el Detalle del evento (deep-link via EXTRA_EVENT_ID
// que lee MainActivity). Si el usuario no concedio POST_NOTIFICATIONS (API 33+), no se publica nada
// (la alarma simplemente no produce aviso visible).
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(EXTRA_EVENT_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (!canPostNotifications(context)) return

        // El eventId viaja en el Intent para que MainActivity abra el Detalle al tocar la notificacion.
        val tapIntent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_EVENT_ID, eventId)
        val contentIntent = PendingIntent.getActivity(
            context,
            ReminderNotifications.notificationId(eventId),
            tapIntent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, ReminderNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(title.ifBlank { context.getString(R.string.reminder_notification_fallback) })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(ReminderNotifications.notificationId(eventId), notification)
    }

    private fun canPostNotifications(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val EXTRA_EVENT_ID = "reminder_event_id"
        const val EXTRA_TITLE = "reminder_title"
    }
}
