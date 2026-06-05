package com.eddndev.purpura.data.mapper

import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder

// Decodifica los codigos ASCII (junta, realizado, ten_minutes_before, ...) a enums del
// dominio. Un codigo desconocido del servidor/cache es una violacion de contrato: se trata
// como DomainError.Unexpected controlado en la frontera de data (06-app-architecture §5.2).
internal object EnumCodec {

    fun eventType(code: String): EventType =
        runCatching { EventType.valueOf(code) }.getOrElse { throw DomainError.Unexpected(it) }

    fun eventStatus(code: String): EventStatus =
        runCatching { EventStatus.valueOf(code) }.getOrElse { throw DomainError.Unexpected(it) }

    fun reminder(code: String): Reminder =
        runCatching { Reminder.valueOf(code) }.getOrElse { throw DomainError.Unexpected(it) }

    fun authProvider(code: String): AuthProvider =
        runCatching { AuthProvider.valueOf(code) }.getOrElse { throw DomainError.Unexpected(it) }

    // El backend modela ausencia como "" (contrato §1.2); el dominio usa null.
    fun emptyToNull(value: String?): String? = value?.takeIf { it.isNotEmpty() }
}
