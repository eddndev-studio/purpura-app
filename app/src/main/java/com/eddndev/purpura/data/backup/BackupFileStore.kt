package com.eddndev.purpura.data.backup

import com.eddndev.purpura.domain.backup.ExportDocument
import java.io.InputStream
import java.io.OutputStream

// Puerto de (de)serializacion del archivo de respaldo. La eleccion del destino/origen (Drive,
// Dropbox o local) la resuelve el Storage Access Framework EN EL FRAGMENT; aqui solo llega el
// stream ya abierto, via las lambdas `open`. Asi `android.net.Uri` no entra ni al ViewModel ni a
// esta capa, que quedan puras y unit-testeables sin Robolectric. La impl cierra el stream.
interface BackupFileStore {

    // Escribe [document] como JSON (formato del export del servidor) en el destino de [open].
    suspend fun write(document: ExportDocument, open: () -> OutputStream)

    // Lee y parsea un documento de respaldo desde [open]. Lanza [InvalidBackupFileException] si
    // el contenido no es un respaldo valido de Purpura (JSON mal formado, esquema ajeno, fechas o
    // enums invalidos). Los fallos de E/S reales (no se pudo abrir el stream) se propagan tal cual.
    suspend fun read(open: () -> InputStream): ExportDocument
}

// El archivo elegido no es un respaldo valido de Purpura. Se mapea a un aviso especifico en la UI
// (distinto de un error de red o inesperado).
class InvalidBackupFileException(cause: Throwable? = null) :
    Exception("invalid backup file", cause)
