package com.eddndev.purpura.di

import com.eddndev.purpura.data.reminder.AlarmManagerReminderScheduler
import com.eddndev.purpura.domain.repository.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Enlaza el puerto de recordatorios a su implementacion real con AlarmManager (REQ-NOTIF-001).
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: AlarmManagerReminderScheduler): ReminderScheduler
}
