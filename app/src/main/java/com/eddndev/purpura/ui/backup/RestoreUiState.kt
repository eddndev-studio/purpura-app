package com.eddndev.purpura.ui.backup

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.repository.CloudBackup

// Estado de la pantalla de Restaurar. `result` (resumen del import) y `errorRes` son avisos de un
// solo uso, limpiados por resultShown()/errorShown(). `driveBackups` (lista de respaldos en la nube)
// se consume al abrir el dialogo de seleccion (driveBackupsShown()); vive aqui para sobrevivir la
// rotacion mientras el dialogo esta abierto.
data class RestoreUiState(
    val isWorking: Boolean = false,
    val result: ImportResult? = null,
    val driveBackups: List<CloudBackup>? = null,
    @StringRes val errorRes: Int? = null,
)
