package com.eddndev.purpura.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.R
import com.eddndev.purpura.data.backup.BackupFileStore
import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.usecase.backup.ExportEventsUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.time.LocalDate
import javax.inject.Inject

// Respaldo (REQ-BACKUP-001). Flujo de dos pasos por el Storage Access Framework:
//   prepareBackup() -> trae el documento del servidor (export). Si esta vacio avisa y no crea
//     archivo; si tiene eventos, lo guarda en memoria y publica pendingFileName para que el
//     Fragment abra el selector del sistema.
//   saveBackup(open) -> el usuario ya eligio destino; escribe el documento ya en memoria.
// Asi el archivo solo se crea cuando hay datos que escribir (nada de archivos de 0 bytes en Drive
// si el export falla por red).
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportEvents: ExportEventsUseCase,
    private val fileStore: BackupFileStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    // Documento traido en el paso 1, a la espera del destino del paso 2. No va al UiState para no
    // arrastrar la lista de eventos por el StateFlow.
    private var pending: ExportDocument? = null

    fun prepareBackup() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorRes = null, infoRes = null) }
        viewModelScope.launch {
            runCatching { exportEvents() }
                .onSuccess { document ->
                    if (document.count == 0) {
                        _uiState.update { it.copy(isWorking = false, infoRes = R.string.backup_empty) }
                    } else {
                        pending = document
                        // Sigue isWorking=true: el trabajo continua en el selector y saveBackup().
                        _uiState.update { it.copy(pendingFileName = suggestedFileName()) }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isWorking = false, errorRes = throwable.toErrorMessageRes())
                    }
                }
        }
    }

    // El Fragment ya abrio el selector con pendingFileName; se consume para no relanzarlo.
    fun launchHandled() {
        _uiState.update { it.copy(pendingFileName = null) }
    }

    fun saveBackup(open: () -> OutputStream) {
        val document = pending ?: return
        viewModelScope.launch {
            runCatching { fileStore.write(document, open) }
                .onSuccess {
                    pending = null
                    _uiState.update { it.copy(isWorking = false, savedCount = document.count) }
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    pending = null
                    // Escribir es E/S local hacia el Uri elegido (Drive sincroniza, etc.): un aviso
                    // de archivo es mas honesto que el generico de "ocurrio un error".
                    _uiState.update { it.copy(isWorking = false, errorRes = R.string.backup_error_save) }
                }
        }
    }

    // El usuario cerro el selector sin elegir destino: libera el documento y el estado de trabajo.
    fun backupCancelled() {
        pending = null
        _uiState.update { it.copy(isWorking = false) }
    }

    fun messageShown() {
        _uiState.update { it.copy(savedCount = null, infoRes = null, errorRes = null) }
    }

    // Nombre sugerido en el selector: fecha ISO (ASCII) + .json. El usuario puede cambiarlo.
    private fun suggestedFileName(): String = "purpura-respaldo-${LocalDate.now()}.json"
}
