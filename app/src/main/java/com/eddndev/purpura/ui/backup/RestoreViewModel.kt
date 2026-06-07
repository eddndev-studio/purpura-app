package com.eddndev.purpura.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eddndev.purpura.R
import com.eddndev.purpura.data.backup.BackupFileStore
import com.eddndev.purpura.data.backup.InvalidBackupFileException
import com.eddndev.purpura.domain.backup.ImportMode
import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.repository.CloudBackupRepository
import com.eddndev.purpura.domain.repository.DriveNotAuthorizedException
import com.eddndev.purpura.domain.usecase.backup.ImportEventsUseCase
import com.eddndev.purpura.ui.common.toErrorMessageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.inject.Inject

// Restaurar (REQ-BACKUP-002). Lee el archivo elegido en el selector (parse local) y aplica sus
// eventos contra la cuenta del usuario. El backend fuerza la propiedad al token (ignora el userId
// del archivo) y aplica upsert por id: restaurar COMBINA, no reemplaza.
@HiltViewModel
class RestoreViewModel @Inject constructor(
    private val importEvents: ImportEventsUseCase,
    private val fileStore: BackupFileStore,
    private val cloudBackup: CloudBackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestoreUiState())
    val uiState: StateFlow<RestoreUiState> = _uiState.asStateFlow()

    // Lista los respaldos de Purpura en Google Drive para que el usuario elija cual restaurar. El
    // Fragment asegura la autorizacion de Google antes de llamar. La lista vive en el UiState (no en
    // el Fragment) para sobrevivir la rotacion mientras el dialogo de seleccion esta abierto.
    fun loadDriveBackups() {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorRes = null, result = null) }
        viewModelScope.launch {
            runCatching { cloudBackup.list() }
                .onSuccess { backups -> _uiState.update { it.copy(isWorking = false, driveBackups = backups) } }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.update { it.copy(isWorking = false, errorRes = driveErrorRes(throwable)) }
                }
        }
    }

    // Restaura desde un respaldo de Drive ya elegido: descarga, parsea (mismo codec/validacion que el
    // camino de archivo) e importa en modo `partial` (combina por id, igual que restaurar de archivo).
    fun restoreFromDrive(id: String) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorRes = null, result = null) }
        viewModelScope.launch {
            runCatching {
                val document = cloudBackup.download(id)
                importEvents(ImportRequest(mode = ImportMode.partial, events = document.events))
            }
                .onSuccess { result -> _uiState.update { it.copy(isWorking = false, result = result) } }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val messageRes = if (throwable is InvalidBackupFileException) {
                        R.string.restore_error_invalid_file
                    } else {
                        driveErrorRes(throwable)
                    }
                    _uiState.update { it.copy(isWorking = false, errorRes = messageRes) }
                }
        }
    }

    // El Fragment consume la lista de Drive (abre el dialogo de seleccion) y la limpia.
    fun driveBackupsShown() {
        _uiState.update { it.copy(driveBackups = null) }
    }

    private fun driveErrorRes(throwable: Throwable): Int =
        if (throwable is DriveNotAuthorizedException) R.string.restore_drive_auth_needed
        else throwable.toErrorMessageRes()

    fun restoreFrom(open: () -> InputStream) {
        if (_uiState.value.isWorking) return
        _uiState.update { it.copy(isWorking = true, errorRes = null, result = null) }
        viewModelScope.launch {
            runCatching {
                val document = fileStore.read(open)
                // Modo `partial` a proposito: aplica los validos y reporta el resto. Como el archivo
                // viene del mismo esquema, los fallos son raros; cuando los hay, el desglose del
                // ImportResult (que solo es significativo en partial) es mas util que un atomico que
                // lo bloquea todo por un registro. El parse invalido ya se ataja antes (read()).
                importEvents(ImportRequest(mode = ImportMode.partial, events = document.events))
            }
                .onSuccess { result -> _uiState.update { it.copy(isWorking = false, result = result) } }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    val messageRes = if (throwable is InvalidBackupFileException) {
                        R.string.restore_error_invalid_file
                    } else {
                        throwable.toErrorMessageRes()
                    }
                    _uiState.update { it.copy(isWorking = false, errorRes = messageRes) }
                }
        }
    }

    fun resultShown() {
        _uiState.update { it.copy(result = null) }
    }

    fun errorShown() {
        _uiState.update { it.copy(errorRes = null) }
    }
}
