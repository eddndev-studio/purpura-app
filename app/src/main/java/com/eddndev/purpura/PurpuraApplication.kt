package com.eddndev.purpura

import android.app.Application
import android.content.pm.PackageManager
import com.eddndev.purpura.data.reminder.ReminderNotifications
import com.google.android.libraries.places.api.Places
import dagger.hilt.android.HiltAndroidApp

// Punto de entrada de Hilt (06-app-architecture §7). @HiltAndroidApp genera el grafo de
// dependencias raiz (SingletonComponent). Crea el canal de notificaciones de recordatorios
// (REQ-NOTIF-001) al iniciar; con minSdk 26 el canal es obligatorio.
@HiltAndroidApp
class PurpuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotifications.ensureChannel(this)
        initPlaces()
    }

    // Places (Autocomplete New) reusa la MISMA key de Maps inyectada en el manifest
    // (com.google.android.geo.API_KEY). Si la key esta vacia (build sin key) NO se inicializa:
    // el selector de ubicacion oculta la busqueda pero la app no rompe. Requiere que la key
    // tenga habilitado el servicio Places API (New) en Google Cloud.
    private fun initPlaces() {
        val key = mapsApiKey() ?: return
        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, key)
        }
    }

    private fun mapsApiKey(): String? {
        @Suppress("DEPRECATION")
        val info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        return info.metaData?.getString("com.google.android.geo.API_KEY")?.takeIf { it.isNotBlank() }
    }
}
