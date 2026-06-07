package com.eddndev.purpura.domain.repository

import com.eddndev.purpura.domain.backup.ExportDocument
import java.time.Instant

// Puerto del dominio para el respaldo PROGRAMATICO en la nube (REQ-BACKUP, API de Google Drive).
// Complementa al camino por archivo (Storage Access Framework): aqui la app sube/baja el documento
// de respaldo directamente contra el almacen del usuario, sin pasar por el selector del sistema.
// La capa data lo implementa con el SDK de Drive (scope drive.file: solo ve los archivos que la
// propia app creo). La autorizacion de Google es independiente de la sesion de Purpura: un usuario
// de correo/contrasena tambien puede respaldar en SU Drive (la cuenta de Google es solo almacen).
interface CloudBackupRepository {

    // Sube [document] como un archivo nuevo con nombre [fileName]. Devuelve la referencia creada.
    suspend fun upload(fileName: String, document: ExportDocument): CloudBackup

    // Lista los respaldos de Purpura que la app creo en la nube, mas reciente primero.
    suspend fun list(): List<CloudBackup>

    // Descarga y parsea el respaldo [id]. Lanza el mismo error de archivo invalido que el codec si
    // el contenido no es un respaldo valido de Purpura.
    suspend fun download(id: String): ExportDocument
}

// Referencia a un archivo de respaldo en la nube (metadatos para listar y elegir cual restaurar).
data class CloudBackup(
    val id: String,
    val name: String,
    val modifiedAt: Instant,
)

// La cuenta de Google no esta autorizada para Drive (sin cuenta o sin el scope drive.file). La capa
// de UI la usa como senal para lanzar el consentimiento de Google antes de reintentar.
class DriveNotAuthorizedException : Exception("google drive not authorized")
