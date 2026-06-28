package com.eddndev.purpura.data.repository

import com.eddndev.purpura.data.local.dao.EventDao
import com.eddndev.purpura.data.remote.mapper.AuthRemoteMapper
import com.eddndev.purpura.data.session.TokenStore
import com.eddndev.purpura.di.IoDispatcher
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.domain.model.User
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

    // Monitor que serializa TODA mutacion de token + sessionState. invalidate() corre en el hilo de
    // OkHttp (401), mientras persist/updateUser/clear corren en IO/Main: sin este lock, el patron
    // check-then-act de updateUser (leer token -> guardar -> emitir) podria intercalarse con un
    // invalidate() concurrente y "resucitar" una sesion ya invalidada. Cada bloque hace su lectura y
    // su escritura DENTRO de la misma seccion critica (no a traves del salto de dispatcher).
    private val sessionLock = Any()

    override fun observeSession(): Flow<Session?> = sessionState.asStateFlow()

    override suspend fun persist(result: AuthResult) {
        withContext(io) {
            synchronized(sessionLock) {
                tokenStore.save(result.accessToken, authMapper.toUserDto(result.user))
                sessionState.value = Session(result.accessToken, result.user)
            }
        }
    }

    // Refresca SOLO el usuario cacheado tras vincular/desvincular Google: reusa el token vigente (no
    // hay token nuevo). No-op si no hay sesion. La lectura del token y la escritura van bajo el mismo
    // lock que invalidate(), asi que un 401 concurrente no puede colarse entre ambas: o invalidate
    // ya corrio (peekToken es null -> no-op) o correra despues (deja la sesion en null, como debe).
    override suspend fun updateUser(user: User) {
        withContext(io) {
            synchronized(sessionLock) {
                val token = tokenStore.peekToken() ?: return@synchronized
                tokenStore.save(token, authMapper.toUserDto(user))
                sessionState.value = Session(token, user)
            }
        }
    }

    override suspend fun currentToken(): String? = tokenStore.peekToken()

    override suspend fun clear() {
        withContext(io) {
            synchronized(sessionLock) {
                tokenStore.clear()
                sessionState.value = null
            }
            // Fuera del lock: limpiar Room no compite con la resurreccion de sesion y puede ser lento.
            eventDao.clear()
        }
    }

    // Token expirado/invalido (401): se llama desde el hilo de OkHttp, asi que es SINCRONA y NO toca
    // Room. Bajo el lock borra el token (apply() es async a disco pero thread-safe) y emite sesion
    // null; el gate de MainActivity reacciona y lleva a Auth. El cache de Room se limpia en el proximo
    // login/logout.
    override fun invalidate() {
        synchronized(sessionLock) {
            tokenStore.clear()
            sessionState.value = null
        }
    }

    private fun readSession(): Session? {
        val token = tokenStore.peekToken() ?: return null
        val userDto = tokenStore.readUser() ?: return null
        return Session(token, authMapper.toUser(userDto))
    }
}
