package com.eddndev.purpura.ui.support

import com.eddndev.purpura.data.backup.BackupFileStore
import com.eddndev.purpura.domain.backup.ExportDocument
import java.io.InputStream
import java.io.OutputStream

// Fake del codec de archivo de respaldo. Ignora las lambdas de stream (las pruebas validan la
// orquestacion del ViewModel, no el I/O real) y permite inyectar resultado o error por operacion.
internal class FakeBackupFileStore : BackupFileStore {

    val written = mutableListOf<ExportDocument>()
    var writeError: Throwable? = null

    var readResult: ExportDocument? = null
    var readError: Throwable? = null

    override suspend fun write(document: ExportDocument, open: () -> OutputStream) {
        writeError?.let { throw it }
        written += document
    }

    override suspend fun read(open: () -> InputStream): ExportDocument {
        readError?.let { throw it }
        return readResult ?: error("readResult no configurado en el fake")
    }
}
