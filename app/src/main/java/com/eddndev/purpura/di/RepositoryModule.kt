package com.eddndev.purpura.di

import com.eddndev.purpura.data.repository.AuthRepositoryImpl
import com.eddndev.purpura.data.repository.EventRepositoryImpl
import com.eddndev.purpura.data.repository.SessionRepositoryImpl
import com.eddndev.purpura.domain.repository.AuthRepository
import com.eddndev.purpura.domain.repository.EventRepository
import com.eddndev.purpura.domain.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Enlaza puerto del dominio -> implementacion de data (06-app-architecture §7.1). Es el
// unico lugar donde ui y data se "encuentran": Hilt inyecta la impl donde el dominio declara
// el puerto.
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(impl: EventRepositoryImpl): EventRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSessionRepository(impl: SessionRepositoryImpl): SessionRepository
}
