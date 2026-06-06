package com.eddndev.purpura.ui.location

import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentLocationPickerBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Selector de ubicacion en mapa (REQ-ADD-005). El usuario toca el mapa para colocar el marcador y
// confirma; devuelve lat/lng (+ una etiqueta por geocodificacion inversa, best-effort) al formulario
// via Fragment Result API. No usa el permiso de ubicacion (no hay capa "mi ubicacion"): solo tap.
//
// La key de Maps se inyecta en el manifest desde local.properties/env; si esta vacia los tiles no
// renderizan (pantalla en gris) pero la app no rompe. El render solo se verifica en un emulador con
// Google Play services firmado con el SHA-1 registrado.
class LocationPickerFragment : Fragment() {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selected: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selected = readSelected(savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(::onMapReady)

        binding.confirmButton.setOnClickListener { confirmSelection() }
    }

    // getMapAsync puede llamar de vuelta despues de que la vista ya no existe: hay que null-guardear.
    private fun onMapReady(map: GoogleMap) {
        val binding = _binding ?: return
        googleMap = map
        val current = selected
        if (current != null) {
            map.addMarker(MarkerOptions().position(current))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, MARKER_ZOOM))
            binding.confirmButton.isVisible = true
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM))
        }
        map.setOnMapClickListener(::placeMarker)
    }

    private fun placeMarker(latLng: LatLng) {
        val map = googleMap ?: return
        val binding = _binding ?: return
        selected = latLng
        map.clear()
        map.addMarker(MarkerOptions().position(latLng))
        binding.confirmButton.isVisible = true
    }

    private fun confirmSelection() {
        val latLng = selected ?: return
        binding.confirmButton.isEnabled = false
        val appContext = requireContext().applicationContext
        viewLifecycleOwner.lifecycleScope.launch {
            val label = reverseGeocode(appContext, latLng)
            setFragmentResult(
                REQUEST_KEY,
                bundleOf(
                    RESULT_LAT to latLng.latitude,
                    RESULT_LNG to latLng.longitude,
                    RESULT_LABEL to label,
                ),
            )
            findNavController().navigateUp()
        }
    }

    // Geocodificacion inversa best-effort: nunca bloquea Confirmar. Si el dispositivo no tiene
    // backend de geocoder o falla, devuelve null y el formulario conserva la etiqueta tecleada.
    private suspend fun reverseGeocode(context: Context, latLng: LatLng): String? =
        withContext(Dispatchers.IO) {
            if (!Geocoder.isPresent()) return@withContext null
            runCatching {
                @Suppress("DEPRECATION")
                Geocoder(context, Locale("es", "MX"))
                    .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    ?.firstOrNull()
                    ?.getAddressLine(0)
            }.getOrNull()
        }

    private fun readSelected(state: Bundle?): LatLng? {
        if (state != null && state.containsKey(KEY_SELECTED_LAT)) {
            return LatLng(state.getDouble(KEY_SELECTED_LAT), state.getDouble(KEY_SELECTED_LNG))
        }
        val lat = arguments?.getDouble(ARG_LAT, Double.NaN) ?: Double.NaN
        val lng = arguments?.getDouble(ARG_LNG, Double.NaN) ?: Double.NaN
        return if (!lat.isNaN() && !lng.isNaN()) LatLng(lat, lng) else null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selected?.let {
            outState.putDouble(KEY_SELECTED_LAT, it.latitude)
            outState.putDouble(KEY_SELECTED_LNG, it.longitude)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "location_picker_result"
        const val RESULT_LAT = "lat"
        const val RESULT_LNG = "lng"
        const val RESULT_LABEL = "label"
        const val ARG_LAT = "arg_lat"
        const val ARG_LNG = "arg_lng"

        private const val KEY_SELECTED_LAT = "selected_lat"
        private const val KEY_SELECTED_LNG = "selected_lng"
        private const val DEFAULT_ZOOM = 10f
        private const val MARKER_ZOOM = 15f
        private val DEFAULT_CENTER = LatLng(19.4326, -99.1332) // CDMX
    }
}
