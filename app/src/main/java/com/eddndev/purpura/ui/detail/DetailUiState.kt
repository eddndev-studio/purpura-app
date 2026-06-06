package com.eddndev.purpura.ui.detail

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.model.Event

// Estado de Detalle. `event` null + isLoading = cargando; `isWorking` cubre cambio de estatus o
// eliminacion en vuelo (para bloquear acciones); `errorRes` es un aviso de un solo uso que NO
// borra el evento; `deleted` es una senal de un solo uso para que el Fragment regrese atras.
data class DetailUiState(
    val event: Event?,
    val isLoading: Boolean,
    val isWorking: Boolean,
    @StringRes val errorRes: Int?,
    val loadFailed: Boolean,
    val deleted: Boolean,
) {
    companion object {
        val Initial = DetailUiState(
            event = null,
            isLoading = true,
            isWorking = false,
            errorRes = null,
            loadFailed = false,
            deleted = false,
        )
    }
}
