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
    val createdAt: String,
)
