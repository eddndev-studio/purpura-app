package com.eddndev.purpura.domain.backup

import com.eddndev.purpura.domain.model.Event
import java.time.Instant

// Documento de respaldo (contrato §5.11). Es lo que el cliente sube a Drive/Dropbox.
data class ExportDocument(
    val schemaVersion: String,
    val exportedAt: Instant,
    val userId: String,
    val count: Int,
    val events: List<Event>,
)

// Modo de aplicacion de la restauracion (contrato §5.12). `partial` aplica los validos y
// reporta los invalidos; `atomic` es todo o nada.
enum class ImportMode { partial, atomic }

// Peticion de restauracion. El `userId` de cada evento lo ignora el backend (propiedad
// forzada al `sub` del token).
data class ImportRequest(
    val mode: ImportMode = ImportMode.partial,
    val events: List<Event>,
)

// Resumen del resultado de importar (contrato §5.12).
data class ImportResult(
    val imported: Int,
    val updated: Int,
    val skipped: Int,
    val failed: Int,
    val errors: List<ImportError>,
)

data class ImportError(
    val index: Int,
    val code: String,
    val detail: String,
)
