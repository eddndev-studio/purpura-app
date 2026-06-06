package com.eddndev.purpura.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.data.reminder.ReminderReceiver
import com.eddndev.purpura.databinding.ActivityMainBinding
import com.eddndev.purpura.domain.usecase.auth.ObserveSessionUseCase
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

// Unica Activity (single-activity). Hospeda el andamiaje: DrawerLayout + Toolbar +
// NavHostFragment + BottomNavigationView + NavigationView. El drawer (7) y el bottom
// nav (3) conducen al mismo NavController; "Consultar" y "Salir" se comportan igual
// desde ambas superficies (REQ-NAV-003).
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var observeSession: ObserveSessionUseCase

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHost.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.addEventFragment,
                R.id.queryFragment,
                R.id.calendarFragment,
                R.id.heatmapFragment,
                R.id.backupFragment,
                R.id.restoreFragment,
                R.id.aboutFragment,
            ),
            binding.drawerLayout,
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            val handled = navigateTo(item.itemId)
            binding.drawerLayout.closeDrawers()
            handled
        }
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            navigateTo(item.itemId)
        }

        hideChromeOnAuth()
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

    // El andamiaje (toolbar + bottom nav + drawer) no aplica en la pantalla de autenticacion.
    private fun hideChromeOnAuth() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuth = destination.id == R.id.authFragment
            binding.appBar.isVisible = !isAuth
            binding.bottomNavigation.isVisible = !isAuth
            binding.drawerLayout.setDrawerLockMode(
                if (isAuth) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED,
            )
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

    private fun navigateTo(itemId: Int): Boolean {
        when (itemId) {
            R.id.nav_add_event -> navController.navigate(R.id.addEventFragment)
            R.id.nav_query_events, R.id.menu_consultar -> navController.navigate(R.id.queryFragment)
            R.id.nav_calendar -> navController.navigate(R.id.calendarFragment)
            R.id.nav_backup -> navController.navigate(R.id.backupFragment)
            R.id.nav_restore -> navController.navigate(R.id.restoreFragment)
            R.id.nav_about -> navController.navigate(R.id.aboutFragment)
            R.id.menu_inicio -> navController.navigate(R.id.homeFragment)
            R.id.nav_exit, R.id.menu_salir -> showExitDialog()
            else -> return false
        }
        return true
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setNegativeButton(R.string.exit_cancel, null)
            .setPositiveButton(R.string.exit_confirm) { _, _ -> finish() }
            .show()
    }

    override fun onSupportNavigateUp(): Boolean =
        navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
}
