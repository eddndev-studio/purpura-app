package com.eddndev.purpura.ui.common

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R

// Inyecta en la toolbar el acceso al Mapa de calor (REQ-NAV-001: ya no vive en el drawer). Lo usan
// Inicio y Calendario. El MenuProvider se ata al viewLifecycleOwner, asi solo aparece mientras el
// fragmento esta resumido. La navegacion se guarda contra doble-tap exigiendo seguir en el origen.
fun Fragment.addHeatmapMenu(fromDestinationId: Int) {
    requireActivity().addMenuProvider(
        object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.menu_heatmap, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                if (item.itemId != R.id.action_heatmap) return false
                val controller = findNavController()
                if (controller.currentDestination?.id == fromDestinationId) {
                    controller.navigate(R.id.heatmapFragment)
                }
                return true
            }
        },
        viewLifecycleOwner,
        Lifecycle.State.RESUMED,
    )
}
