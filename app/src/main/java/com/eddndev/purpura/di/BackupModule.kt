package com.eddndev.purpura.di

import com.eddndev.purpura.data.backup.BackupFileStore
import com.eddndev.purpura.data.backup.JsonBackupFileStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Enlaza el puerto de archivo de respaldo con su impl JSON (sin estado, singleton). Separado de
// RepositoryModule porque BackupFileStore no es un repositorio del dominio, sino un codec de la
// capa data.
@Module
@InstallIn(SingletonComponent::class)
abstract class BackupModule {

    @Binds
    @Singleton
    abstract fun bindBackupFileStore(impl: JsonBackupFileStore): BackupFileStore
}
