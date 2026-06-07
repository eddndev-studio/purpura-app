package com.eddndev.purpura.ui.common

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes

// Autorizacion de Google SOLO para el almacenamiento en Drive (scope drive.file). Es independiente de
// la sesion de Purpura: un usuario de correo/contrasena tambien puede vincular su Google aqui para
// respaldar en SU Drive. El consentimiento (signInIntent) debe lanzarlo el Fragment con un
// ActivityResultLauncher; este helper solo arma el cliente y consulta si ya hay permiso.
object DriveAuth {

    private val DRIVE_SCOPE = Scope(DriveScopes.DRIVE_FILE)

    // True si hay una cuenta de Google con el scope drive.file ya concedido -> se puede usar Drive sin
    // volver a pedir consentimiento.
    fun isAuthorized(context: Context): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
        return GoogleSignIn.hasPermissions(account, DRIVE_SCOPE)
    }

    // Cliente con el scope de Drive solicitado; su signInIntent lanza el selector de cuenta + el
    // consentimiento incremental (Google solo muestra el permiso nuevo si ya habia sesion).
    fun client(context: Context): GoogleSignInClient {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_SCOPE)
            .build()
        return GoogleSignIn.getClient(context, options)
    }
}
