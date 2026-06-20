package com.eddndev.purpura.ui.compose

import androidx.compose.runtime.Composable
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.ui.common.EventDisplay
import com.eddndev.purpura.ui.theme.ColorPair
import com.eddndev.purpura.ui.theme.PurpuraTheme

/**
 * Mapeo de los enums del dominio a los colores semanticos de Compose ([ColorPair] de
 * [PurpuraTheme.colors]). Es el equivalente Compose de los `typeColor`/`statusContainer` de
 * [EventDisplay] (que devuelven @ColorRes para Views). Las ETIQUETAS @StringRes se siguen tomando de
 * [EventDisplay] via `stringResource(...)`, evitando duplicar el mapeo de texto.
 */
@Composable
fun colorsFor(type: EventType): ColorPair = with(PurpuraTheme.colors) {
    when (type) {
        EventType.cita -> eventCita
        EventType.junta -> eventJunta
        EventType.entrega_proyecto -> eventEntrega
        EventType.examen -> eventExamen
        EventType.otros -> eventOtros
    }
}

@Composable
fun colorsFor(status: EventStatus): ColorPair = with(PurpuraTheme.colors) {
    when (status) {
        EventStatus.pendiente -> statusPendiente
        EventStatus.realizado -> statusRealizado
        EventStatus.aplazado -> statusAplazado
    }
}
