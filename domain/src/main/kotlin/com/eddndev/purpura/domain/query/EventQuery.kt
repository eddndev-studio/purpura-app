package com.eddndev.purpura.domain.query

import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.QueryMode
import java.time.LocalDate

// Criterios de consulta (contrato §5.7). Cubre las cuatro ventanas temporales (dia, rango,
// mes, anio) mas filtro por tipo/estatus, paginacion y orden. Las fronteras temporales se
// evaluan en UTC salvo que se indique una zona IANA en `tz`.
data class EventQuery(
    val mode: QueryMode? = null,
    val date: LocalDate? = null,
    val from: LocalDate? = null,
    val to: LocalDate? = null,
    val year: Int? = null,
    val month: Int? = null,
    val type: EventType? = null,
    val status: EventStatus? = null,
    val page: Int = 1,
    val pageSize: Int = 20,
    val sort: String = "startsAt:asc",
    val tz: String? = null,
)
