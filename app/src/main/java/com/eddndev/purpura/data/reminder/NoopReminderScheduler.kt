package com.eddndev.purpura.data.reminder

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.ReminderScheduler
import javax.inject.Inject
import javax.inject.Singleton

// Stand-in del puerto de recordatorios. Existe para que el grafo de Hilt compile: los casos de
// uso de edicion (ChangeStatus/Update/Delete) inyectan ReminderScheduler. Hoy NO programa nada
// porque el unico lugar que define un recordatorio es AddEvent, que aun es placeholder; sin
// AddEvent no hay ningun recordatorio que disparar, asi que un no-op es indistinguible de la
// implementacion real en el estado actual de la app.
//
// TODO(#8 AddEvent): reemplazar por una implementacion real con AlarmManager + BroadcastReceiver
// + canal de notificacion + permisos POST_NOTIFICATIONS/USE_EXACT_ALARM, calculando el instante
// de disparo desde reminder + startsAt (ver ReminderScheduler KDoc). El reemplazo es transparente
// para los casos de uso: solo cambia el binding en ReminderModule.
@Singleton
class NoopReminderScheduler @Inject constructor() : ReminderScheduler {
    override fun schedule(event: Event) = Unit
    override fun cancel(eventId: String) = Unit
}
