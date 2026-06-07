package com.eddndev.purpura.data.backup

import android.content.Context
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.backup.ExportDocument
import com.eddndev.purpura.domain.repository.CloudBackup
import com.eddndev.purpura.domain.repository.CloudBackupRepository
import com.eddndev.purpura.domain.repository.DriveNotAuthorizedException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

// Respaldo/Restauracion PROGRAMATICA en Google Drive (REQ-BACKUP, API de Drive). Implementa el puerto
// del dominio usando el SDK REST de Drive autorizado con la cuenta del Google Sign-In y el scope
// drive.file (la app solo ve/toca los archivos que ella misma crea; list() no vera respaldos de otras
// apps ni de una reinstalacion con otro cliente). La (de)serializacion JSON se delega al mismo
// [BackupFileStore] del camino de archivo, asi el formato del archivo en Drive es byte-compatible con
// el del Storage Access Framework y la validacion de "archivo invalido" se reutiliza tal cual.
//
// Todas las llamadas al SDK son de red BLOQUEANTE -> se confinan al dispatcher de IO. El RENDER y el
// flujo de OAuth/round-trip solo se verifican en device; el build verde unicamente prueba el cableado.
@Singleton
class DriveBackupStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStore: BackupFileStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : CloudBackupRepository {

    override suspend fun upload(fileName: String, document: ExportDocument): CloudBackup = withContext(io) {
        // Serializa con el codec compartido a un buffer en memoria (el archivo es pequeno: solo eventos).
        val buffer = ByteArrayOutputStream()
        fileStore.write(document) { buffer }
        val metadata = DriveFile().setName(fileName).setMimeType(MIME_JSON)
        val content = ByteArrayContent(MIME_JSON, buffer.toByteArray())
        val created = drive().files().create(metadata, content)
            .setFields(FILE_FIELDS)
            .execute()
        created.toCloudBackup()
    }

    override suspend fun list(): List<CloudBackup> = withContext(io) {
        val response = drive().files().list()
            .setSpaces(DRIVE_SPACE)
            // drive.file ya restringe a archivos de la app; el filtro por nombre acota a los respaldos.
            .setQ("trashed = false and mimeType = '$MIME_JSON' and name contains '$NAME_PREFIX'")
            .setOrderBy("modifiedTime desc")
            .setFields("files($FILE_FIELDS)")
            .setPageSize(MAX_RESULTS)
            .execute()
        response.files.orEmpty().map { it.toCloudBackup() }
    }

    override suspend fun download(id: String): ExportDocument = withContext(io) {
        val buffer = ByteArrayOutputStream()
        drive().files().get(id).executeMediaAndDownloadTo(buffer)
        val bytes = buffer.toByteArray()
        // Reusa el codec: lanza InvalidBackupFileException si el contenido no es un respaldo valido.
        fileStore.read { ByteArrayInputStream(bytes) }
    }

    // Construye el cliente de Drive con la cuenta del Google Sign-In actual. Sin cuenta (o sin el
    // scope drive.file concedido) la credencial no tiene cuenta seleccionada -> el llamador debe
    // lanzar el consentimiento antes: aqui se senala con DriveNotAuthorizedException.
    private fun drive(): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: throw DriveNotAuthorizedException()
        val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_FILE))
        credential.selectedAccount = account
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName(APP_NAME)
            .build()
    }

    private fun DriveFile.toCloudBackup(): CloudBackup = CloudBackup(
        id = id,
        name = name ?: id,
        // modifiedTime se solicita en FILE_FIELDS; si faltara, cae al epoch (no rompe el orden visual).
        modifiedAt = modifiedTime?.let { Instant.ofEpochMilli(it.value) } ?: Instant.EPOCH,
    )

    private companion object {
        const val MIME_JSON = "application/json"
        const val NAME_PREFIX = "purpura-respaldo-"
        const val DRIVE_SPACE = "drive"
        const val FILE_FIELDS = "id, name, modifiedTime"
        const val MAX_RESULTS = 50
        const val APP_NAME = "Purpura"
    }
}
