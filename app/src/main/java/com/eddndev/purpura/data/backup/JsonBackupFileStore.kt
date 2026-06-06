package com.eddndev.purpura.data.backup

import com.eddndev.purpura.data.remote.dto.ExportDocumentDto
import com.eddndev.purpura.data.remote.mapper.EventRemoteMapper
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.backup.ExportDocument
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

// Codec JSON del archivo de respaldo sobre streams (06-app-architecture, backup §5.11/§5.12).
// Reusa el mismo Moshi + DTOs + mapper de la API, asi el archivo es byte-compatible con el export
// del servidor y no se reinventan adaptadores de Instant/enum. El I/O corre en el dispatcher de IO.
class JsonBackupFileStore @Inject constructor(
    moshi: Moshi,
    private val mapper: EventRemoteMapper,
    @IoDispatcher private val io: CoroutineDispatcher,
) : BackupFileStore {

    // Indentado para que el respaldo sea legible si el usuario lo abre en Drive/Dropbox.
    private val adapter = moshi.adapter(ExportDocumentDto::class.java).indent("  ")

    override suspend fun write(document: ExportDocument, open: () -> OutputStream) = withContext(io) {
        val json = adapter.toJson(mapper.toExportDocumentDto(document))
        open().use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }

    override suspend fun read(open: () -> InputStream): ExportDocument = withContext(io) {
        // Falla de E/S al abrir/leer -> se propaga (problema real de archivo, no de formato).
        val json = open().use { it.readBytes().toString(Charsets.UTF_8) }
        // De aqui en adelante todo es CPU (sin puntos de suspension), por eso runCatching NO se
        // traga ninguna CancellationException: cualquier fallo es de formato -> archivo invalido.
        val dto = runCatching { adapter.fromJson(json) }
            .getOrElse { throw InvalidBackupFileException(it) }
            ?: throw InvalidBackupFileException()
        runCatching { mapper.toExportDocument(dto) }
            .getOrElse { throw InvalidBackupFileException(it) }
    }
}
