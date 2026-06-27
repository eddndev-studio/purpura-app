package com.eddndev.purpura.ui.location

import android.app.Activity
import android.content.Context
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentLocationPickerBinding
import com.eddndev.purpura.ui.common.MapStyling
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

// Selector de ubicacion en mapa (REQ-ADD-005). Dos formas de elegir: (1) BUSCAR un lugar por nombre
// con el autocompletado de Places (Autocomplete New) y (2) TOCAR el mapa para marcar a mano. Confirma
// y devuelve lat/lng (+ etiqueta) al formulario via Fragment Result API. No usa permiso de ubicacion.
//
// La key de Maps/Places se inyecta en el manifest desde local.properties/env; si esta vacia los tiles
// no renderizan y la busqueda se oculta (Places no inicializa), pero la app no rompe. Render+busqueda
// solo se verifican en un device/emulador con Google Play services firmado con el SHA-1 registrado.
class LocationPickerFragment : Fragment() {

    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

    private var googleMap: GoogleMap? = null
    private var selected: LatLng? = null
    // Etiqueta proveniente de la busqueda de Places. Si esta presente, Confirmar la usa en vez de
    // geocodificar; un tap manual la limpia para volver a la geocodificacion inversa.
    private var selectedLabel: String? = null

    private var placesClient: PlacesClient? = null
    private var sessionToken: AutocompleteSessionToken? = null

    private val autocompleteLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onAutocompleteResult(result)
        }

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
        applyEdgeToEdgeInsets()
        selected = readSelected(savedInstanceState)
        selectedLabel = savedInstanceState?.getString(KEY_SELECTED_LABEL)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
        mapFragment?.getMapAsync(::onMapReady)

        // La busqueda solo aparece si Places quedo inicializado (key presente + Places API habilitada).
        placesClient = if (Places.isInitialized()) {
            runCatching { Places.createClient(requireContext()) }.getOrNull()
        } else {
            null
        }
        binding.searchBar.isVisible = placesClient != null
        binding.searchBar.setOnClickListener { launchSearch() }

        binding.confirmButton.setOnClickListener { confirmSelection() }
    }

    // Edge-to-edge (la Activity es enableEdgeToEdge): el mapa ocupa toda la pantalla a proposito
    // (tiles bajo la status/nav bar), pero la barra de busqueda flotante y el boton Confirmar deben
    // respetar las barras del sistema o quedan tapados (la barra de busqueda se metia bajo la status
    // bar). Sumamos el inset de cada barra al margen base de cada control.
    private fun applyEdgeToEdgeInsets() {
        val density = resources.displayMetrics.density
        val searchBase = (SEARCH_BAR_MARGIN_DP * density).toInt()
        val confirmBase = (CONFIRM_MARGIN_DP * density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.searchBar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = bars.top + searchBase
            }
            binding.confirmButton.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bars.bottom + confirmBase
            }
            insets
        }
    }

    // getMapAsync puede llamar de vuelta despues de que la vista ya no existe: hay que null-guardear.
    private fun onMapReady(map: GoogleMap) {
        val binding = _binding ?: return
        googleMap = map
        // Estilo de marca (oculta POIs, tine el mapa, dia/noche): quita el look de Maps crudo.
        MapStyling.apply(requireContext(), map)
        val current = selected
        if (current != null) {
            map.addMarker(MapStyling.markerOptions(current))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(current, MARKER_ZOOM))
            binding.confirmButton.isVisible = true
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_CENTER, DEFAULT_ZOOM))
        }
        map.setOnMapClickListener(::placeMarker)
    }

    // Abre el widget de autocompletado. El session token agrupa las pulsaciones + la seleccion en una
    // sesion de facturacion; se cierra al pedir los detalles del lugar con el MISMO token.
    //
    // Sin filtro de tipos => devuelve TODO (negocios, puntos de interes y direcciones). Para que los
    // NEGOCIOS cercanos salgan (y no solo direcciones), sesgamos la busqueda a lo que se ve en el mapa:
    // locationBias = la region visible y origin = el centro, asi el ranking prioriza lo local. Si el
    // mapa aun no esta listo, cae a una busqueda nacional (MX) sin sesgo.
    private fun launchSearch() {
        if (placesClient == null) return
        val token = AutocompleteSessionToken.newInstance()
        sessionToken = token
        val visibleBounds = googleMap?.projection?.visibleRegion?.latLngBounds
        val center = googleMap?.cameraPosition?.target
        val intent = PlaceAutocomplete.createIntent(requireContext()) {
            setAutocompleteSessionToken(token)
            setCountries(listOf("MX"))
            visibleBounds?.let { setLocationBias(RectangularBounds.newInstance(it)) }
            center?.let { setOrigin(it) }
        }
        autocompleteLauncher.launch(intent)
    }

    private fun onAutocompleteResult(result: ActivityResult) {
        if (result.resultCode != Activity.RESULT_OK) return
        val data = result.data ?: return
        val placeId = PlaceAutocomplete.getPredictionFromIntent(data)?.placeId ?: return
        fetchPlace(placeId)
    }

    // Place Details para obtener coordenadas + nombre/direccion. Campos Essentials (gratis a nuestra
    // escala). El token cierra la sesion de autocompletado.
    private fun fetchPlace(placeId: String) {
        val client = placesClient ?: return
        val fields = listOf(
            Place.Field.ID,
            Place.Field.DISPLAY_NAME,
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION,
        )
        val request = FetchPlaceRequest.builder(placeId, fields)
            .setSessionToken(sessionToken)
            .build()
        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                if (_binding == null) return@addOnSuccessListener
                val place = response.place
                val location = place.location ?: return@addOnSuccessListener
                applySelection(location, place.displayName ?: place.formattedAddress)
            }
            .addOnFailureListener {
                val binding = _binding ?: return@addOnFailureListener
                Snackbar.make(binding.root, R.string.location_search_error, Snackbar.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { sessionToken = null }
    }

    // Coloca el marcador desde la busqueda: recuerda la etiqueta del lugar y acerca la camara.
    private fun applySelection(latLng: LatLng, label: String?) {
        val map = googleMap ?: return
        val binding = _binding ?: return
        selected = latLng
        selectedLabel = label
        map.clear()
        map.addMarker(MapStyling.markerOptions(latLng))
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, MARKER_ZOOM))
        binding.confirmButton.isVisible = true
    }

    private fun placeMarker(latLng: LatLng) {
        val map = googleMap ?: return
        val binding = _binding ?: return
        selected = latLng
        selectedLabel = null // tap manual: volver a geocodificar al confirmar
        map.clear()
        map.addMarker(MapStyling.markerOptions(latLng))
        binding.confirmButton.isVisible = true
    }

    private fun confirmSelection() {
        val latLng = selected ?: return
        binding.confirmButton.isEnabled = false
        val appContext = requireContext().applicationContext
        val knownLabel = selectedLabel
        viewLifecycleOwner.lifecycleScope.launch {
            val label = knownLabel ?: reverseGeocode(appContext, latLng)
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
        selectedLabel?.let { outState.putString(KEY_SELECTED_LABEL, it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        googleMap = null
        placesClient = null
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
        private const val KEY_SELECTED_LABEL = "selected_label"
        private const val DEFAULT_ZOOM = 10f
        private const val MARKER_ZOOM = 15f
        // Margenes base (los del layout) a los que se suma el inset de la barra del sistema.
        private const val SEARCH_BAR_MARGIN_DP = 12f
        private const val CONFIRM_MARGIN_DP = 24f
        private val DEFAULT_CENTER = LatLng(19.4326, -99.1332) // CDMX
    }
}
