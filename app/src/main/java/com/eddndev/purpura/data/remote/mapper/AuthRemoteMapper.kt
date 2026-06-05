package com.eddndev.purpura.data.remote.mapper

import com.eddndev.purpura.data.mapper.EnumCodec
import com.eddndev.purpura.data.remote.dto.AuthResponseDto
import com.eddndev.purpura.data.remote.dto.UserDto
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

    fun toUser(dto: UserDto): User = User(
        id = dto.id,
        email = dto.email,
        nombre = dto.nombre,
        authProvider = EnumCodec.authProvider(dto.authProvider),
        createdAt = Instant.parse(dto.createdAt),
    )

    fun toUserDto(user: User): UserDto = UserDto(
        id = user.id,
        email = user.email,
        nombre = user.nombre,
        authProvider = user.authProvider.name,
        createdAt = user.createdAt.toString(),
    )
}
