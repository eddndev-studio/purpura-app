package com.eddndev.purpura.domain.model

import java.time.Instant

data class User(
    val id: String,
    val email: String,
    val nombre: String,
    val authProvider: AuthProvider,
    val createdAt: Instant,
)
