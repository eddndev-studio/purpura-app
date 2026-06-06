package com.eddndev.purpura.ui.backup

import androidx.annotation.StringRes
import com.eddndev.purpura.domain.backup.ImportResult

// Estado de la pantalla de Restaurar. `result` (resumen del import) y `errorRes` son avisos de un
// solo uso, limpiados por resultShown()/errorShown().
data class RestoreUiState(
    val isWorking: Boolean = false,
    val result: ImportResult? = null,
    @StringRes val errorRes: Int? = null,
)
