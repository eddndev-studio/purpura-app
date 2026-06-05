package com.eddndev.purpura.data.remote.dto

// DTOs de respaldo/restauracion (contrato §5.11/§5.12). Forma plana de evento, identica a
// listar. El userId de los eventos importados lo ignora el backend.

data class ExportDocumentDto(
    val schemaVersion: String,
    val exportedAt: String,
    val userId: String,
    val count: Int,
    val events: List<EventDto>,
)

data class ImportRequestDto(
    val mode: String,
    val events: List<EventDto>,
)

data class ImportResultDto(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<ImportErrorDto> = emptyList(),
)

data class ImportErrorDto(
    val index: Int,
    val code: String,
    val detail: String,
)
