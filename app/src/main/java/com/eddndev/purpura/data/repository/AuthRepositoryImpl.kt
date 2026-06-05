package com.eddndev.purpura.data.repository

import com.eddndev.purpura.data.remote.api.AuthApi
import com.eddndev.purpura.data.remote.dto.GoogleAuthRequest
import com.eddndev.purpura.data.remote.dto.LoginRequest
import com.eddndev.purpura.data.remote.dto.RegisterRequest
import com.eddndev.purpura.data.remote.interceptor.ProblemErrorAdapter
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

// Llama a /auth/* y mapea a AuthResult/DomainError. La persistencia de la sesion la hace el
// caso de uso (LoginUseCase, etc.) via SessionRepository, no este repositorio.
class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
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
}
