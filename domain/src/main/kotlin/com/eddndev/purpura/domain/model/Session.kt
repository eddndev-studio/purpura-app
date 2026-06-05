package com.eddndev.purpura.domain.model

// Sesion local: el JWT propio de Purpura mas el usuario al que pertenece. `null` (ausente)
// significa no autenticado, lo que lleva la navegacion a AuthFragment (06-app-architecture
// §8.1). El token se adjunta como `Authorization: Bearer <token>` en cada peticion.
data class Session(
    val token: String,
    val user: User,
)

// Resultado de un endpoint de autenticacion (contrato §5.2/§5.3/§5.4): access token,
// vida en segundos y el usuario. Se persiste como Session.
data class AuthResult(
    val accessToken: String,
    val expiresInSeconds: Long,
    val user: User,
)
