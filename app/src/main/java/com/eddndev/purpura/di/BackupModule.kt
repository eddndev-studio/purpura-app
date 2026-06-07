package com.eddndev.purpura.di

import com.eddndev.purpura.data.backup.BackupFileStore
import com.eddndev.purpura.data.backup.DriveBackupStore
import com.eddndev.purpura.data.backup.JsonBackupFileStore
import com.eddndev.purpura.domain.repository.CloudBackupRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Enlaza el codec de archivo de respaldo (JSON) y el repositorio de respaldo en la nube (Drive).
// Separado de RepositoryModule: BackupFileStore es un codec de la capa data, y CloudBackupRepository
// es un puerto del dominio implementado con el SDK de Drive.
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupFileStore(impl: JsonBackupFileStore): BackupFileStore

    @Binds
    @Singleton
    abstract fun bindCloudBackupRepository(impl: DriveBackupStore): CloudBackupRepository
}
