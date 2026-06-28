package com.eddndev.purpura.data.remote.dto

// DTOs de autenticacion (contrato §5.2..§5.4). Claves camelCase.

data class RegisterRequest(
    val email: String,
    val nombre: String,
    val password: String,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class GoogleAuthRequest(
    val idToken: String,
)

data class AuthResponseDto(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserDto,
)

data class UserDto(
    val id: String,
    val email: String,
    val nombre: String,
    val authProvider: String,
    // Default false: el usuario cacheado por versiones anteriores no traia esta clave; Moshi usa el
    // default al deserializar ese JSON viejo. La invariante "origen Google => vinculado" la reconstruye
    // el mapper, asi que un cache previo a esta version no muestra Vincular a una cuenta Google.
    val googleLinked: Boolean = false,
    // Default TRUE para el cache viejo (sin esta clave): preferimos NO mostrar el aviso "verifica tu
    // correo" a un usuario que quiza ya esta verificado (o es de origen Google) que mostrarlo de mas.
    // El valor real llega del backend en login/me; Inicio refresca via GET /auth/me al reanudar.
    val emailVerified: Boolean = true,
    val createdAt: String,
)
