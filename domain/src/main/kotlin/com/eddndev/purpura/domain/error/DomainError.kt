package com.eddndev.purpura.domain.error

// Jerarquia sellada espejo del modelo de error del contrato (04-api-contract §4).
// Mapea uno a uno los `code` machine-readable para que la UI decida su mensaje sin
// parsear prosa HTTP. Las invariantes del dominio lanzan estos mismos errores antes
// de tocar la red, de modo que validacion local y backend hablen el mismo idioma.
sealed class DomainError(
    override val message: String? = null,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    data class Validation(val fieldErrors: List<FieldError> = emptyList()) : DomainError("validation_failed")

    data object InvalidEventType : DomainError("invalid_event_type")     // 422
    data object InvalidStatus : DomainError("invalid_status")            // 422
    data object InvalidReminder : DomainError("invalid_reminder")        // 422
    data object EmptyDescription : DomainError("empty_description")      // 422
    data object InvalidLocation : DomainError("invalid_location")        // 422
    data object EventNotFound : DomainError("event_not_found")           // 404
    data object UserNotFound : DomainError("user_not_found")             // 404
    data object EmailTaken : DomainError("email_taken")                  // 409
    data object GoogleLinkConflict : DomainError("google_link_conflict") // 409: el Google ya esta en otra cuenta
    data object CannotUnlinkGoogle : DomainError("cannot_unlink_google") // 409: quedaria sin credencial de acceso
    data object EmailNotVerified : DomainError("email_not_verified")     // 403: correo de Google sin verificar
    data object InvalidGoogleToken : DomainError("invalid_google_token") // 400: idToken de Google no valido
    data object InvalidCredential : DomainError("invalid_credential")    // 401
    data object Unauthorized : DomainError("unauthorized")               // 401
    data object Network : DomainError("network")                         // sin conexion / timeout
    data class Unexpected(val original: Throwable?) : DomainError("unexpected", original)

    data class FieldError(val field: String, val message: String)
}
