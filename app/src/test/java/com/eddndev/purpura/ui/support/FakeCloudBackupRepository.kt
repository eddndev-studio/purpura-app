package com.eddndev.purpura.ui.support

import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.repository.CloudBackup
import com.eddndev.purpura.domain.repository.CloudBackupRepository
import java.time.Instant

// Fake del repositorio de respaldo en la nube. Registra lo subido/descargado y permite inyectar
// resultado o error por operacion, para probar la orquestacion de los ViewModel sin tocar Drive.
internal class FakeCloudBackupRepository : CloudBackupRepository {

    val uploaded = mutableListOf<Pair<String, ExportDocument>>()
    var uploadError: Throwable? = null
    var uploadResult: CloudBackup = CloudBackup("drive-1", "purpura-respaldo-2026-06-07.json", Instant.EPOCH)

    var listResult: List<CloudBackup> = emptyList()
    var listError: Throwable? = null

    val downloaded = mutableListOf<String>()
    var downloadResult: ExportDocument? = null
    var downloadError: Throwable? = null

    override suspend fun upload(fileName: String, document: ExportDocument): CloudBackup {
        uploadError?.let { throw it }
        uploaded += fileName to document
        return uploadResult
    }

    override suspend fun list(): List<CloudBackup> {
        listError?.let { throw it }
        return listResult
    }

    override suspend fun download(id: String): ExportDocument {
        downloadError?.let { throw it }
        downloaded += id
        return downloadResult ?: error("downloadResult no configurado en el fake")
    }
}
