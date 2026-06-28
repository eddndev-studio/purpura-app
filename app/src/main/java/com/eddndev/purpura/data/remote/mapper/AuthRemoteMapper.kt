package com.eddndev.purpura.data.remote.mapper

import com.eddndev.purpura.data.mapper.EnumCodec
import com.eddndev.purpura.data.remote.dto.AuthResponseDto
import com.eddndev.purpura.data.remote.dto.UserDto
import com.eddndev.purpura.domain.model.AuthProvider
import com.eddndev.purpura.domain.model.AuthResult
import com.eddndev.purpura.domain.model.User
import java.time.Instant
import javax.inject.Inject

// Traduce los DTO de autenticacion <-> dominio. `toUserDto` se usa para cachear el usuario
// en el almacenamiento seguro de sesion.
class AuthRemoteMapper @Inject constructor() {

    fun toAuthResult(dto: AuthResponseDto): AuthResult = AuthResult(
        accessToken = dto.accessToken,
        expiresInSeconds = dto.expiresIn,
        user = toUser(dto.user),
    )

    fun toUser(dto: UserDto): User {
        val authProvider = EnumCodec.authProvider(dto.authProvider)
        return User(
            id = dto.id,
            email = dto.email,
            nombre = dto.nombre,
            authProvider = authProvider,
            // Una cuenta de ORIGEN Google siempre tiene Google adjunto (el backend garantiza
            // google_sub != null). Derivarlo aqui reconstruye esa invariante para un cache viejo
            // (sin la clave googleLinked) sin esperar al siguiente login.
            googleLinked = dto.googleLinked || authProvider == AuthProvider.google,
            createdAt = Instant.parse(dto.createdAt),
        )
    }

    fun toUserDto(user: User): UserDto = UserDto(
        id = user.id,
        email = user.email,
        nombre = user.nombre,
        authProvider = user.authProvider.name,
        googleLinked = user.googleLinked,
        createdAt = user.createdAt.toString(),
    )
}
