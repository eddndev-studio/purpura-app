package com.eddndev.purpura.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.data.reminder.ReminderReceiver
import com.eddndev.purpura.databinding.ActivityMainBinding
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Unica Activity (single-activity). Edge-to-edge: solo hospeda NavHostFragment +
// BottomNavigationView; el app bar vive en cada pantalla Compose. La bottom nav (4 destinos top
// level) conduce el NavController via setupWithNavController, que maneja navegacion, seleccion y
// back stack. Respaldo/Restaurar/Acerca/Cerrar sesion viven dentro de "Cuenta" (ya no en un drawer).
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var observeSession: ObserveSessionUseCase

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        binding.bottomNavigation.setupWithNavController(navController)

        toggleChromeByDestination()
        observeSessionGate()
        handleReminderDeepLink(intent)
    }

    // Si la app vuelve a primer plano por el tap de un recordatorio (no por relanzamiento limpio),
    // el nuevo Intent llega aqui (FLAG_ACTIVITY_SINGLE_TOP). setIntent lo deja como intent actual.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderDeepLink(intent)
    }

    // Deep-link de notificacion: abre el Detalle del evento que disparo el recordatorio. Consume el
    // extra (removeExtra) para no reabrir el Detalle al rotar (onCreate corre de nuevo con el mismo
    // Intent). Si la sesion expiro, el gate ya habra llevado a Auth: no abrimos Detalle sobre ella.
    private fun handleReminderDeepLink(intent: Intent) {
        val eventId = intent.getStringExtra(ReminderReceiver.EXTRA_EVENT_ID) ?: return
        intent.removeExtra(ReminderReceiver.EXTRA_EVENT_ID)
        if (navController.currentDestination?.id == R.id.authFragment) return
        navController.navigate(R.id.eventDetailFragment, bundleOf(ARG_EVENT_ID to eventId))
    }

    // La bottom nav solo aparece en los destinos de nivel superior. En el resto (Auth, Detalle,
    // formulario, Respaldo/Restaurar/Acerca, Mapa de calor, selector de ubicacion) se oculta para
    // que cada pantalla ocupe todo y maneje su propio back desde el TopAppBar.
    private fun toggleChromeByDestination() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.isVisible = destination.id in TOP_LEVEL_DESTINATIONS
        }
    }

    // El estado de sesion gobierna el destino (06-app-architecture §8.1). Sin mutar el
    // startDestination: se observa y se navega. null -> Auth; sesion -> Inicio.
    private fun observeSessionGate() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                observeSession().collect { session ->
                    val current = navController.currentDestination?.id
                    if (session == null && current != R.id.authFragment) {
                        navController.navigate(
                            R.id.authFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(navController.graph.startDestinationId, true)
                                .build(),
                        )
                    } else if (session != null && current == R.id.authFragment) {
                        navController.navigate(
                            R.id.homeFragment,
                            null,
                            NavOptions.Builder().setPopUpTo(R.id.authFragment, true).build(),
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val TOP_LEVEL_DESTINATIONS = setOf(
            R.id.homeFragment,
            R.id.calendarFragment,
            R.id.queryFragment,
            R.id.accountFragment,
        )
    }
}
