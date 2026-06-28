package com.eddndev.purpura.data.repository

import com.eddndev.purpura.data.remote.api.AccountApi
import com.eddndev.purpura.data.remote.api.AuthApi
import com.eddndev.purpura.data.remote.dto.GoogleAuthRequest
import com.eddndev.purpura.data.remote.dto.LoginRequest
import com.eddndev.purpura.data.remote.dto.RegisterRequest
import com.eddndev.purpura.data.remote.interceptor.ProblemErrorAdapter
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.User
import com.eddndev.purpura.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject

// Llama a /auth/* y mapea a AuthResult/DomainError. La persistencia de la sesion la hace el
// caso de uso (LoginUseCase, etc.) via SessionRepository, no este repositorio.
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val accountApi: AccountApi,
    private val authMapper: AuthRemoteMapper,
    private val errorAdapter: ProblemErrorAdapter,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun register(email: String, nombre: String, password: String): AuthResult =
        withContext(io) {
            errorAdapter.call {
                authMapper.toAuthResult(authApi.register(RegisterRequest(email, nombre, password)))
            }
        }

    override suspend fun login(email: String, password: String): AuthResult =
        withContext(io) {
            errorAdapter.call {
                authMapper.toAuthResult(authApi.login(LoginRequest(email, password)))
            }
        }

    override suspend fun loginWithGoogle(idToken: String): AuthResult =
        withContext(io) {
            errorAdapter.call {
                authMapper.toAuthResult(authApi.google(GoogleAuthRequest(idToken)))
            }
        }

    // Borra la cuenta en el backend (204 esperado). Como es Response<Unit>, Retrofit no lanza por
    // un status no-2xx: lo convertimos en HttpException para que el errorAdapter lo mapee a
    // DomainError (mismo patron que EventRepositoryImpl.delete). No toca la sesion local.
    override suspend fun deleteAccount() {
        withContext(io) {
            errorAdapter.call {
                val response = accountApi.deleteAccount()
                if (!response.isSuccessful) throw HttpException(response)
            }
        }
    }

    // Vincular/Desvincular Google devuelven el usuario actualizado (200 con cuerpo). El refresco de
    // la sesion local (conservando el token) lo hace el caso de uso via SessionRepository.
    override suspend fun linkGoogle(idToken: String): User =
        withContext(io) {
            errorAdapter.call {
                authMapper.toUser(accountApi.linkGoogle(GoogleAuthRequest(idToken)))
            }
        }

    override suspend fun unlinkGoogle(): User =
        withContext(io) {
            errorAdapter.call {
                authMapper.toUser(accountApi.unlinkGoogle())
            }
        }

    // GET /auth/me: usuario fresco (200 con cuerpo). El caso de uso refresca la sesion local.
    override suspend fun me(): User =
        withContext(io) {
            errorAdapter.call {
                authMapper.toUser(authApi.me())
            }
        }

    // POST /auth/verify-email/request (202 sin cuerpo). Como es Response<Unit>, Retrofit no lanza por
    // un status no-2xx: lo convertimos en HttpException para que el errorAdapter lo mapee a
    // DomainError (mismo patron que deleteAccount). El backend identifica al usuario por la sesion.
    override suspend fun requestEmailVerification() {
        withContext(io) {
            errorAdapter.call {
                val response = authApi.requestEmailVerification()
                if (!response.isSuccessful) throw HttpException(response)
            }
        }
    }
}
