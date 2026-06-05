package com.eddndev.purpura.domain.model

// Codigos en INGLES (consistencia critica con el contrato). Las etiquetas de UI
// en espanol viven en recursos string, nunca como identificador de codigo.
enum class Reminder {
    none,
    at_time,
    ten_minutes_before,
    one_day_before,
}
