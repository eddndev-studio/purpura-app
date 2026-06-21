package com.eddndev.purpura.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.Bundle
import com.eddndev.purpura.ui.common.MapStyling
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng

/**
 * Mapa lite de solo lectura para Compose: hospeda un [MapView] (lite mode) via [AndroidView],
 * reenvia su ciclo de vida al del composable y aplica el estilo de marca + el marcador morado. Lo
 * usa el Detalle para mostrar la ubicacion del evento sin la complejidad de maps-compose.
 */
@Composable
fun LiteLocationMap(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val latLng = remember(lat, lng) { LatLng(lat, lng) }
    val mapView = remember { MapView(context, GoogleMapOptions().liteMode(true)) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        // addObserver reenvia los eventos necesarios para llevar al MapView al estado actual del
        // ciclo de vida (CREATE/START/RESUME), asi que onCreate se invoca aqui sin doble llamada.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier) { mv ->
        mv.getMapAsync { map ->
            MapStyling.apply(context, map)
            map.clear()
            map.addMarker(MapStyling.markerOptions(latLng))
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DETAIL_ZOOM))
            map.uiSettings.isMapToolbarEnabled = false
        }
    }
}

private const val DETAIL_ZOOM = 15f
