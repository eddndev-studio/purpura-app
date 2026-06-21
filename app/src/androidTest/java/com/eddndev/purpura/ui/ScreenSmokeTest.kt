package com.eddndev.purpura.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.about.AboutScreen
import com.eddndev.purpura.ui.addevent.AddEventScreen
import com.eddndev.purpura.ui.addevent.AddEventUiState
import com.eddndev.purpura.ui.auth.AuthScreen
import com.eddndev.purpura.ui.auth.AuthUiState
import com.eddndev.purpura.ui.backup.BackupScreen
import com.eddndev.purpura.ui.backup.BackupUiState
import com.eddndev.purpura.ui.backup.RestoreScreen
import com.eddndev.purpura.ui.backup.RestoreUiState
import com.eddndev.purpura.ui.calendar.CalendarCell
import com.eddndev.purpura.ui.calendar.CalendarScreen
import com.eddndev.purpura.ui.calendar.CalendarUiState
import com.eddndev.purpura.ui.detail.DetailUiState
import com.eddndev.purpura.ui.detail.EventDetailScreen
import com.eddndev.purpura.ui.heatmap.HeatmapCell
import com.eddndev.purpura.ui.heatmap.HeatmapScreen
import com.eddndev.purpura.ui.heatmap.HeatmapUiState
import com.eddndev.purpura.ui.home.HomeScreen
import com.eddndev.purpura.ui.home.HomeUiState
import com.eddndev.purpura.ui.query.QueryScreen
import com.eddndev.purpura.ui.query.QueryUiState
import com.eddndev.purpura.ui.theme.PurpuraTheme
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth

/**
 * Smoke test de runtime: monta CADA pantalla migrada a Compose con un estado de ejemplo y verifica
 * que COMPONE Y MIDE sin crashear (waitForIdle fuerza composicion + layout). Cubre los riesgos que el
 * compilador no ve: bucles de efectos, y sobre todo los grids de Calendario/Mapa de calor (riesgo de
 * "measured with infinity maximum height" por scroll anidado). Las pantallas con mapa se prueban sin
 * coordenadas (location 0,0) para no depender de Google Play services en el emulador.
 */
class ScreenSmokeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleEvent(id: String = "1") = Event(
        id = id,
        userId = "u1",
        type = EventType.cita,
        contact = Contact(name = "Maria Gonzalez", ref = null),
        location = Location(lat = 0.0, lng = 0.0, label = "Biblioteca central"),
        description = "Revision de avance del proyecto",
        startsAt = Instant.parse("2026-06-21T18:00:00Z"),
        status = EventStatus.pendiente,
        reminder = Reminder.none,
        createdAt = Instant.parse("2026-06-20T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-20T10:00:00Z"),
    )

    private fun render(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeTestRule.setContent { PurpuraTheme { content() } }
        composeTestRule.waitForIdle()
    }

    @Test
    fun home_renders() = render {
        HomeScreen(
            state = HomeUiState(events = listOf(sampleEvent(), sampleEvent("2")), isLoading = false, errorRes = null),
            onRefresh = {}, onAddEvent = {}, onEventClick = {}, onErrorShown = {},
        )
    }

    @Test
    fun home_empty_renders() = render {
        HomeScreen(state = HomeUiState.Initial.copy(isLoading = false), onRefresh = {}, onAddEvent = {}, onEventClick = {}, onErrorShown = {})
    }

    @Test
    fun about_renders() = render { AboutScreen(versionName = "0.0.0", onBack = {}, onLogout = {}) }

    @Test
    fun auth_renders() = render {
        AuthScreen(state = AuthUiState.Idle, onLogin = { _, _ -> }, onRegister = { _, _, _ -> }, onGoogleClick = {}, onErrorShown = {})
    }

    @Test
    fun backup_renders() = render {
        BackupScreen(state = BackupUiState(), onBack = {}, onBackupToDrive = {}, onBackupToFile = {}, onMessageShown = {})
    }

    @Test
    fun restore_renders() = render {
        RestoreScreen(state = RestoreUiState(), onBack = {}, onRestoreFromDrive = {}, onRestoreFromFile = {}, onResultShown = {}, onErrorShown = {})
    }

    @Test
    fun calendar_renders() = render {
        val today = LocalDate.now()
        val cells = buildList {
            add(CalendarCell.Empty)
            repeat(30) { i ->
                add(CalendarCell.Day(date = today.withDayOfMonth(1).plusDays(i.toLong()), typeDots = listOf(EventType.cita), eventCount = i % 3, isToday = i == today.dayOfMonth - 1, isSelected = i == 0))
            }
        }
        CalendarScreen(
            state = CalendarUiState(yearMonth = YearMonth.now(), cells = cells, selectedDate = today, selectedDayEvents = listOf(sampleEvent()), isLoading = false, errorRes = null),
            onSelectDate = {}, onPrevMonth = {}, onNextMonth = {}, onEventClick = {}, onErrorShown = {}, onShowHeatmap = {},
        )
    }

    @Test
    fun heatmap_renders() = render {
        val today = LocalDate.now()
        val cells = buildList {
            add(HeatmapCell.Empty)
            repeat(30) { i ->
                add(HeatmapCell.Day(date = today.withDayOfMonth(1).plusDays(i.toLong()), count = i % 5, level = i % 5, isToday = i == today.dayOfMonth - 1))
            }
        }
        HeatmapScreen(
            state = HeatmapUiState(yearMonth = YearMonth.now(), cells = cells, totalEvents = 12, isLoading = false, errorRes = null),
            onPrevMonth = {}, onNextMonth = {}, onShowCalendar = {}, onErrorShown = {},
        )
    }

    @Test
    fun query_renders() = render {
        QueryScreen(
            state = QueryUiState.Initial.copy(events = listOf(sampleEvent()), isLoading = false, hasSearched = true),
            onSearch = {}, onLoadMore = {}, onPickDate = {}, onEventClick = {}, onErrorShown = {},
        )
    }

    @Test
    fun detail_renders() = render {
        EventDetailScreen(
            state = DetailUiState(event = sampleEvent(), isLoading = false, isWorking = false, errorRes = null, loadFailed = false, deleted = false),
            onBack = {}, onRetry = {}, onChangeStatus = {}, onEdit = {}, onDelete = {}, onDeleted = {}, onErrorShown = {},
        )
    }

    @Test
    fun addevent_renders() = render {
        AddEventScreen(
            state = AddEventUiState(), externalPicks = emptyFlow(),
            onPickContact = {}, onPickLocation = { _, _ -> }, onSubmit = {}, onClearFieldErrors = {},
            onPrefillHandled = {}, onSaved = {}, onRetryLoad = {}, onErrorShown = {},
        )
    }
}
