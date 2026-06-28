package com.eddndev.purpura.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val nombre: String,
    val authProvider: AuthProvider,
    // googleLinked: la cuenta tiene una identidad de Google adjunta (puede entrar tambien por
    // Google). Lo decide el backend (google_sub != null) y gobierna mostrar Vincular vs Desvincular
    // en Cuenta. authProvider sigue siendo el proveedor de ORIGEN inmutable; son independientes.
    val googleLinked: Boolean,
    val createdAt: Instant,
)
