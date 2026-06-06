package com.eddndev.purpura.ui.support

import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.repository.ReminderScheduler

// Fake del programador de recordatorios: registra lo que se programa/cancela para que las
// pruebas de los casos de uso de edicion verifiquen el efecto sin tocar AlarmManager.
internal class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<Event>()
    val cancelled = mutableListOf<String>()

    override fun schedule(event: Event) {
        scheduled += event
    }

    override fun cancel(eventId: String) {
        cancelled += eventId
    }
}
