package com.eddndev.purpura.ui.common

import android.content.Context
import android.content.res.Configuration
import com.eddndev.purpura.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

// Estilo de marca para Google Maps, compartido por el selector de ubicacion y el mapa lite del
// Detalle. Resuelve el "look de Google Maps crudo": oculta POIs comerciales, suaviza la geometria y
// tine el mapa hacia el morado, con variante de dia y de noche acorde al tema. El marcador usa el
// tono violeta de la marca en vez del rojo por defecto.
object MapStyling {

    // Tono del marcador (HSV hue ~ violeta). defaultMarker(hue) reusa el pin del sistema tintado, sin
    // necesidad de un asset propio.
    const val MARKER_HUE = 275f

    fun apply(context: Context, map: GoogleMap) {
        val night = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val styleRes = if (night) R.raw.map_style_purpura_night else R.raw.map_style_purpura
        runCatching { map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, styleRes)) }
    }

    fun markerOptions(latLng: LatLng): MarkerOptions =
        MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(MARKER_HUE))
}
