package com.eddndev.purpura.data.repository

import com.eddndev.purpura.data.local.dao.EventDao
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.data.session.TokenStore
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.repository.SessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Envuelve el almacenamiento seguro de sesion y expone Flow<Session?>. Al cerrar sesion
// vacia tambien el cache de Room para no filtrar datos entre cuentas (06-app-architecture
// §9, regla 6).
@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val tokenStore: TokenStore,
    private val eventDao: EventDao,
    private val authMapper: AuthRemoteMapper,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SessionRepository {

    private val sessionState = MutableStateFlow(readSession())

    override fun observeSession(): Flow<Session?> = sessionState.asStateFlow()

    override suspend fun persist(result: AuthResult) {
        withContext(io) {
            tokenStore.save(result.accessToken, authMapper.toUserDto(result.user))
        }
        sessionState.value = Session(result.accessToken, result.user)
    }

    override suspend fun currentToken(): String? = tokenStore.peekToken()

    override suspend fun clear() {
        withContext(io) {
            tokenStore.clear()
            eventDao.clear()
        }
        sessionState.value = null
    }

    private fun readSession(): Session? {
        val token = tokenStore.peekToken() ?: return null
        val userDto = tokenStore.readUser() ?: return null
        return Session(token, authMapper.toUser(userDto))
    }
}
