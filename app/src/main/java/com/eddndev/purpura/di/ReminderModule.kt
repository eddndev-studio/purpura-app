package com.eddndev.purpura.di

import com.eddndev.purpura.data.reminder.NoopReminderScheduler
import com.eddndev.purpura.domain.repository.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Enlaza el puerto de recordatorios a su stand-in actual (NoopReminderScheduler). Aislado en su
// propio modulo para que al construir AddEvent (#8) se cambie aqui una sola linea por el binding
// de la implementacion real con AlarmManager.
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: NoopReminderScheduler): ReminderScheduler
}
