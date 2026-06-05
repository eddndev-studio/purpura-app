package com.eddndev.purpura.domain.query

// Envoltorio paginado generico (contrato §6). Espejo de `{ data, pagination }`.
data class Page<T>(
    val items: List<T>,
    val pagination: Pagination,
)

data class Pagination(
    val page: Int,
    val pageSize: Int,
    val totalItems: Int,
    val totalPages: Int,
    val sort: String,
)
