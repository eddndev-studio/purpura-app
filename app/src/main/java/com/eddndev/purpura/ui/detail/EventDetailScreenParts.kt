package com.eddndev.purpura.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.compose.EventTypeBadge
import com.eddndev.purpura.ui.compose.InfoCard
import com.eddndev.purpura.ui.compose.InfoRow
import com.eddndev.purpura.ui.compose.MapCard
import com.eddndev.purpura.ui.compose.SectionHeader
import com.eddndev.purpura.ui.compose.SegmentedToggle
import com.eddndev.purpura.ui.theme.Elevation
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing

// Hero del mapa en el Detalle (spec §7): mas alto que en formularios/listas para que la ubicacion
// "lidere" visualmente sin sentirse un thumbnail.
private val MapHeroHeight = 210.dp

/**
 * Cuerpo del Detalle. Entra con un fade+slide sutil (~220ms) para que el contenido "asiente" tras el
 * Crossfade de fase, en lugar de aparecer de golpe. El slide nace desde abajo (1/12 de su alto) para
 * leerse como un ascenso, no un salto. El padding superior (lg) separa la cabecera del app bar.
 */
@Composable
internal fun DetailContent(
    event: Event,
    working: Boolean,
    onChangeStatus: (EventStatus) -> Unit,
    onOpenMap: (() -> Unit)?,
) {
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = visibleState,
        enter = fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 12 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screenH)
                .padding(top = Spacing.lg, bottom = Spacing.xl),
        ) {
            DetailHeader(event)

            val hasCoordinates = event.location.lat != 0.0 || event.location.lng != 0.0
            val locationLabel = event.location.label?.takeIf { it.isNotBlank() }
            // El mapa pulido (hero) solo con coordenadas reales; la affordance "Abrir en Maps"
            // (spec §7) aparece solo si el Fragment cableo onOpenMap.
            if (hasCoordinates) {
                Spacer(Modifier.height(Spacing.section))
                MapCard(
                    lat = event.location.lat,
                    lng = event.location.lng,
                    label = locationLabel,
                    height = MapHeroHeight,
                    onOpenExternal = onOpenMap,
                )
            }

            Spacer(Modifier.height(Spacing.section))
            DetailInfo(event, locationLabel, hasCoordinates)

            Spacer(Modifier.height(Spacing.section))
            StatusSection(current = event.status, working = working, onChangeStatus = onChangeStatus)
        }
    }
}

// Cabecera con jerarquia de identidad (spec §4): primero el "que" (descripcion como titular), luego el
// "cuando" (fecha discreta) y la hora destacada en primary; el tipo cierra como etiqueta de contexto.
@Composable
private fun DetailHeader(event: Event) {
    Column {
        Text(
            text = event.description,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = EventDisplay.formatFullDate(event.startsAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxs))
        Text(
            text = EventDisplay.formatTime(event.startsAt),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(Spacing.md))
        EventTypeBadge(event.type)
    }
}

// Grupo "Detalles": filas de info agrupadas en una sola card con divisores suaves, en lugar de filas
// sueltas. Da la lectura de "ficha" coherente con Cuenta.
@Composable
private fun DetailInfo(
    event: Event,
    locationLabel: String?,
    hasCoordinates: Boolean,
) {
    SectionHeader(stringResource(R.string.detail_section_details))
    InfoCard {
        InfoRow(
            icon = Icons.Outlined.Person,
            label = stringResource(R.string.detail_contact_label),
            value = event.contact.name,
        )
        InfoDivider()
        InfoRow(
            icon = Icons.Outlined.Place,
            label = stringResource(R.string.detail_location_label),
            value = locationLabel
                ?: if (hasCoordinates) stringResource(R.string.event_location_on_map)
                else stringResource(R.string.detail_no_location),
        )
        InfoDivider()
        InfoRow(
            icon = Icons.Outlined.Notifications,
            label = stringResource(R.string.detail_reminder_label),
            value = stringResource(EventDisplay.reminderLabel(event.reminder)),
        )
    }
}

// Divisor de baja prominencia entre filas de la InfoCard: outlineVariant para separar sin "cortar".
@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = Spacing.xs),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun StatusSection(
    current: EventStatus,
    working: Boolean,
    onChangeStatus: (EventStatus) -> Unit,
) {
    // labelOf de SegmentedToggle no es @Composable: resolvemos las etiquetas antes (stringResource
    // es valido aqui, dentro del cuerpo composable).
    val labels = EventStatus.entries.associateWith { stringResource(EventDisplay.statusLabel(it)) }
    SectionHeader(stringResource(R.string.detail_status_label))
    SegmentedToggle(
        options = EventStatus.entries,
        selected = current,
        // Evita disparos durante un cambio en vuelo o re-seleccionar el mismo estatus.
        onSelect = { if (!working && it != current) onChangeStatus(it) },
        labelOf = { labels.getValue(it) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Acciones primarias ancladas al pie (slot bottomBar de PurpuraScreen): "Editar" como pildora primaria
 * que domina, y "Eliminar" en estilo de peligro (TextButton en error) con jerarquia secundaria. Pinar
 * las acciones evita perderlas al hacer scroll del contenido. Ambas se bloquean mientras hay trabajo.
 */
@Composable
internal fun DetailBottomBar(
    working: Boolean,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = Elevation.bottomBar,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screenH, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onEdit,
                enabled = !working,
                shape = Pill,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.detail_edit))
            }
            TextButton(
                onClick = onDeleteRequest,
                enabled = !working,
                shape = Pill,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                Spacer(Modifier.size(Spacing.sm))
                Text(stringResource(R.string.detail_delete))
            }
        }
    }
}
