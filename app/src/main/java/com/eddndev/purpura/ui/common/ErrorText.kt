package com.eddndev.purpura.ui.common

import androidx.annotation.StringRes
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError

// Traduce un DomainError al recurso string en espanol que se muestra al usuario
// (06-app-architecture §11). Los errores de validacion por campo se resaltan en su
// TextInputLayout; este mapeo cubre el mensaje general (snackbar).
@StringRes
fun DomainError.toMessageRes(): Int = when (this) {
    is DomainError.Network -> R.string.error_network
    is DomainError.EmailTaken -> R.string.error_email_taken
    is DomainError.InvalidCredential -> R.string.error_invalid_credential
    is DomainError.Unauthorized -> R.string.error_unauthorized
    is DomainError.EventNotFound -> R.string.error_event_not_found
    is DomainError.Validation,
    is DomainError.InvalidEventType,
    is DomainError.InvalidStatus,
    is DomainError.InvalidReminder,
    is DomainError.EmptyDescription,
    is DomainError.InvalidLocation,
    -> R.string.error_validation
    is DomainError.UserNotFound,
    is DomainError.Unexpected,
    -> R.string.error_unexpected
}
