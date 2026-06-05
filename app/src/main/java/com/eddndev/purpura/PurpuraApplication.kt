package com.eddndev.purpura

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Punto de entrada de Hilt (06-app-architecture §7). @HiltAndroidApp genera el grafo de
// dependencias raiz (SingletonComponent). El canal de notificaciones se crea aqui cuando
// se implemente REQ-NOTIF-001.
@HiltAndroidApp
class PurpuraApplication : Application()
