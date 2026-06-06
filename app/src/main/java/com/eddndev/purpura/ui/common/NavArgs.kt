package com.eddndev.purpura.ui.common

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.eddndev.purpura.R

// Claves de argumentos de navegacion compartidas entre destinos (Consultar/Inicio -> Detalle).
const val ARG_EVENT_ID = "eventId"

// Resultado (FragmentResult) que el formulario en modo edicion envia al Detalle al guardar, para que
// este recargue el evento (su load() es idempotente por id y no refrescaria solo).
const val RESULT_EVENT_EDITED = "result_event_edited"

// Navega al Detalle del evento SOLO si el origen sigue siendo el destino actual. Asi un doble-tap
// (o multitouch) sobre la lista no empuja el Detalle dos veces antes de que la transaccion se
// confirme. Mismo idioma de guarda que MainActivity.observeSessionGate.
fun NavController.navigateToEventDetail(eventId: String, fromDestinationId: Int) {
    if (currentDestination?.id == fromDestinationId) {
        navigate(R.id.eventDetailFragment, bundleOf(ARG_EVENT_ID to eventId))
    }
}
