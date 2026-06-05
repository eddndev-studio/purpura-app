package com.eddndev.purpura.domain.model

// Value object. ref es opcional (el lookup key del contacto del telefono).
data class Contact(
    val name: String,
    val ref: String? = null,
)
