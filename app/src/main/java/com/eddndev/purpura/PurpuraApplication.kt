package com.eddndev.purpura

import android.app.Application
import com.eddndev.purpura.data.reminder.ReminderNotifications
import dagger.hilt.android.HiltAndroidApp

// Punto de entrada de Hilt (06-app-architecture §7). @HiltAndroidApp genera el grafo de
// dependencias raiz (SingletonComponent). Crea el canal de notificaciones de recordatorios
// (REQ-NOTIF-001) al iniciar; con minSdk 26 el canal es obligatorio.
@HiltAndroidApp
class PurpuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
    }
}
