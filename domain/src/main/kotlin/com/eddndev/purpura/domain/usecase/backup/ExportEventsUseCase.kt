package com.eddndev.purpura.domain.usecase.backup

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.query.EventQuery
import com.eddndev.purpura.domain.repository.EventRepository
import javax.inject.Inject

// REQ-BACKUP-001. Obtiene el documento de respaldo (todos los eventos o un subconjunto). La
// subida a Drive/Dropbox la hace la capa ui/integracion con el JSON producido aqui.
class ExportEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(query: EventQuery? = null): ExportDocument =
        repository.export(query)
}
