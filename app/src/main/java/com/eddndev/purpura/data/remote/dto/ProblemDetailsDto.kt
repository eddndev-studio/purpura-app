package com.eddndev.purpura.data.remote.dto

// problem+json (RFC 7807, contrato §4). El campo `code` es la extension machine-readable
// que el cliente mapea a DomainError sin parsear prosa.
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val instance: String? = null,
    val code: String? = null,
    val errors: List<FieldErrorDto>? = null,
)

data class FieldErrorDto(
    val field: String,
    val message: String,
)
