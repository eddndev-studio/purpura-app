package com.eddndev.purpura.domain.model

// Value object. label opcional (texto legible de la ubicacion elegida en el mapa).
data class Location(
    val lat: Double,
    val lng: Double,
    val label: String? = null,
)
