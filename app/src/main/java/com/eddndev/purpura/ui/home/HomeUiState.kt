package com.eddndev.purpura.ui.home

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.model.Event

// Estado de Inicio. El cache (Room) es la fuente de la lista; `isLoading` refleja el refresh
// contra la API (arranca en true para no parpadear el estado vacio en frio). `errorRes` es un
// aviso de un solo uso: un refresh fallido no borra la lista cacheada (06-app-architecture §9).
data class HomeUiState(
    val events: List<Event>,
    val isLoading: Boolean,
    @StringRes val errorRes: Int?,
) {
    // Vacio real solo cuando ya no estamos cargando; durante el refresh inicial se suprime.
    val isEmpty: Boolean get() = events.isEmpty() && !isLoading

    companion object {
        val Initial = HomeUiState(events = emptyList(), isLoading = true, errorRes = null)
    }
}
