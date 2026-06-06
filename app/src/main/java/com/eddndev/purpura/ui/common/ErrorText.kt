package com.eddndev.purpura.ui.common

import androidx.annotation.StringRes
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError
import kotlinx.coroutines.CancellationException

// Mapea un fallo cualquiera a @StringRes para el snackbar, PROPAGANDO la cancelacion estructurada.
// Una corrutina superada (p.ej. una nueva busqueda que cancela la anterior, o el teardown del VM)
// se reanuda con CancellationException; eso NO es un error de UI y no debe pintar un aviso.
// runCatching atrapa Throwable (incluida CancellationException), por eso se re-lanza aqui antes de
// mapear, para que la cancelacion siga su curso normal.
@StringRes
fun Throwable.toErrorMessageRes(): Int {
    if (this is CancellationException) throw this
    return (this as? DomainError ?: DomainError.Unexpected(this)).toMessageRes()
}

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
