package com.eddndev.purpura.ui.backup

import androidx.annotation.StringRes

// Estado de la pantalla de Respaldo. El flujo es de dos pasos por el Storage Access Framework:
// (1) se trae el documento del servidor; (2) si hay eventos, [pendingFileName] le pide al Fragment
// abrir el selector del sistema; al volver con el destino se escribe el archivo. `infoRes` y
// `errorRes` son avisos de un solo uso (se limpian con messageShown()). `savedCount` NO: se conserva
// como confirmacion en linea (no un snackbar fugaz) y solo se reinicia al empezar un nuevo respaldo.
data class BackupUiState(
    val isWorking: Boolean = false,
    val pendingFileName: String? = null,
    val savedCount: Int? = null,
    @StringRes val infoRes: Int? = null,
    @StringRes val errorRes: Int? = null,
)
