package com.eddndev.purpura.ui.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import com.eddndev.purpura.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions

// Estilo de marca para Google Maps, compartido por el selector de ubicacion y el mapa lite del
// Detalle. Resuelve el "look de Google Maps crudo": oculta POIs comerciales, suaviza la geometria y
// tine el mapa hacia el morado, con variante de dia y de noche acorde al tema. El marcador es un
// PUNTO morado de marca (circulo relleno + nucleo blanco + sombra suave), no el pin/teardrop rojo
// por defecto, que es lo que delataba el origen "Google Maps crudo".
object MapStyling {

    // Fallback de tono (HSV hue ~ violeta) por si la construccion del bitmap fallara en runtime.
    const val MARKER_HUE = 275f

    // Morado de marca para el punto: vivo para contrastar tanto en el mapa de dia como en el de noche.
    private const val MARKER_FILL = 0xFF7B2AB8.toInt()

    // Descriptor del punto de marca, construido una vez (necesita Maps inicializado) y cacheado.
    private var brandedMarker: BitmapDescriptor? = null

    fun apply(context: Context, map: GoogleMap) {
        val night = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val styleRes = if (night) R.raw.map_style_purpura_night else R.raw.map_style_purpura
        runCatching { map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, styleRes)) }
        // El descriptor se construye aqui porque apply() siempre corre dentro de getMapAsync (Maps ya
        // inicializado) y antes de cualquier addMarker. Cacheado: una sola vez por proceso.
        if (brandedMarker == null) brandedMarker = runCatching { buildMarker(context) }.getOrNull()
    }

    fun markerOptions(latLng: LatLng): MarkerOptions =
        MarkerOptions()
            .position(latLng)
            .anchor(0.5f, 0.5f)
            .icon(brandedMarker ?: BitmapDescriptorFactory.defaultMarker(MARKER_HUE))

    // Dibuja el punto: sombra suave + circulo morado + nucleo blanco. Tamano en px segun densidad.
    private fun buildMarker(context: Context): BitmapDescriptor {
        val density = context.resources.displayMetrics.density
        val sizePx = (30f * density).toInt()
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val cx = sizePx / 2f
        val cy = sizePx / 2f
        val radius = 9f * density

        val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.WHITE
            setShadowLayer(4f * density, 0f, 1.5f * density, 0x55000000)
        }
        // setShadowLayer requiere software layer; en un Bitmap propio funciona directo.
        canvas.drawCircle(cx, cy, radius, shadow)

        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MARKER_FILL }
        canvas.drawCircle(cx, cy, radius, fill)

        val core = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE }
        canvas.drawCircle(cx, cy, radius * 0.42f, core)

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }
}
