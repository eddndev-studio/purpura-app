package com.eddndev.purpura.data.remote.interceptor

import com.eddndev.purpura.data.remote.dto.FieldErrorDto
import com.eddndev.purpura.data.remote.dto.ProblemDetailsDto
import com.eddndev.purpura.domain.error.DomainError
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Centraliza la traduccion de HttpException/IOException/problem+json a DomainError usando el
// campo `code` (contrato §4.1). Vive en data: el dominio nunca ve un HttpException.
@Singleton
class ProblemErrorAdapter @Inject constructor(moshi: Moshi) {

    private val problemAdapter = moshi.adapter(ProblemDetailsDto::class.java)

    // Ejecuta una llamada de API y convierte cualquier fallo a DomainError. Relanza la
    // cancelacion de corrutinas para no romper la cancelacion estructurada.
    suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        throw toDomain(throwable)
    }

    fun toDomain(throwable: Throwable): DomainError = when (throwable) {
        is DomainError -> throwable
        is HttpException -> fromHttp(throwable)
        is IOException -> DomainError.Network
        else -> DomainError.Unexpected(throwable)
    }

    private fun fromHttp(exception: HttpException): DomainError {
        val problem = runCatching {
            exception.response()?.errorBody()?.string()?.takeIf { it.isNotBlank() }?.let {
                problemAdapter.fromJson(it)
            }
        }.getOrNull()

        return when (problem?.code) {
            "invalid_event_type" -> DomainError.InvalidEventType
            "invalid_status" -> DomainError.InvalidStatus
            "invalid_reminder" -> DomainError.InvalidReminder
            "empty_description" -> DomainError.EmptyDescription
            "invalid_location" -> DomainError.InvalidLocation
            "event_not_found" -> DomainError.EventNotFound
            "user_not_found" -> DomainError.UserNotFound
            "email_taken" -> DomainError.EmailTaken
            "google_link_conflict" -> DomainError.GoogleLinkConflict
            "cannot_unlink_google" -> DomainError.CannotUnlinkGoogle
            "email_not_verified" -> DomainError.EmailNotVerified
            "invalid_google_token" -> DomainError.InvalidGoogleToken
            "invalid_credential" -> DomainError.InvalidCredential
            "unauthorized" -> DomainError.Unauthorized
            "validation_failed" -> DomainError.Validation(problem.errors.orEmpty().map { it.toFieldError() })
            else -> fromStatus(exception.code(), problem)
        }
    }

    // Sin `code` reconocible: cae al codigo HTTP (contrato §4.2).
    private fun fromStatus(status: Int, problem: ProblemDetailsDto?): DomainError = when (status) {
        401 -> DomainError.Unauthorized
        404 -> DomainError.EventNotFound
        409 -> DomainError.EmailTaken
        422 -> DomainError.Validation(problem?.errors.orEmpty().map { it.toFieldError() })
        else -> DomainError.Unexpected(null)
    }

    private fun FieldErrorDto.toFieldError() = DomainError.FieldError(field, message)
}
