package com.eddndev.purpura.domain.usecase.backup

import com.eddndev.purpura.domain.backup.ImportRequest
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.repository.EventRepository
import javax.inject.Inject

// REQ-BACKUP-002. Aplica el documento de respaldo descargado de Drive/Dropbox hacia la
// cuenta del usuario. El backend fuerza la propiedad al `sub` del token (contrato §5.12).
class ImportEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(request: ImportRequest): ImportResult =
        repository.import(request)
}
