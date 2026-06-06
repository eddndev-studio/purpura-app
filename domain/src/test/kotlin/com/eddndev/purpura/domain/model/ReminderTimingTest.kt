package com.eddndev.purpura.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

// Cubre la regla de disparo del recordatorio (REQ-NOTIF-001) que consume el programador real.
class ReminderTimingTest {

    private val startsAt = Instant.parse("2026-06-10T15:30:00Z")

    @Test
    fun `none no programa`() {
        assertNull(Reminder.none.triggerAt(startsAt))
    }

    @Test
    fun `at_time dispara a la hora del evento`() {
        assertEquals(startsAt, Reminder.at_time.triggerAt(startsAt))
    }

    @Test
    fun `ten_minutes_before dispara diez minutos antes`() {
        assertEquals(Instant.parse("2026-06-10T15:20:00Z"), Reminder.ten_minutes_before.triggerAt(startsAt))
    }

    @Test
    fun `one_day_before dispara un dia antes`() {
        assertEquals(Instant.parse("2026-06-09T15:30:00Z"), Reminder.one_day_before.triggerAt(startsAt))
    }
}
