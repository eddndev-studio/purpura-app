package com.eddndev.purpura.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.eddndev.purpura.data.remote.dto.UserDto
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Almacenamiento seguro de la sesion: JWT + usuario cacheado (EncryptedSharedPreferences,
// 06-app-architecture §4.2). Lectura sincrona (peekToken) para que el AuthInterceptor
// adjunte el Bearer sin bloquear en corrutina.
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi,
) {
    private val userAdapter = moshi.adapter(UserDto::class.java)
    private val prefs: SharedPreferences = createPrefs(context)

    fun peekToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun readUser(): UserDto? = prefs.getString(KEY_USER, null)
        ?.let { runCatching { userAdapter.fromJson(it) }.getOrNull() }

    fun save(token: String, user: UserDto) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, userAdapter.toJson(user))
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    // Si el archivo cifrado quedo corrupto (cambio de master key), se descarta y se recrea.
    private fun createPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return runCatching { build(context, masterKey) }.getOrElse {
            context.deleteSharedPreferences(FILE_NAME)
            build(context, masterKey)
        }
    }

    private fun build(context: Context, masterKey: MasterKey): SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private companion object {
        const val FILE_NAME = "purpura_session"
        const val KEY_TOKEN = "access_token"
        const val KEY_USER = "user"
    }
}
