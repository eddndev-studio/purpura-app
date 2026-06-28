package com.eddndev.purpura.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Session
import com.eddndev.purpura.ui.theme.CardShape
import com.eddndev.purpura.ui.theme.Spacing

// Piezas hoja de la pantalla Cuenta, extraidas para mantener AccountScreen por debajo del limite de
// 400 LOC y seguir el patron *ScreenParts del resto de pantallas.

// Fila de Inicio de sesion con Google. Reusa el lenguaje visual de PurpuraListRow (tarjeta +
// pastilla de icono) pero con contenido al final segun el estado:
//  - sin vincular: la tarjeta entera es accionable (lanza el flujo de Google) con chevron.
//  - vinculada y con contrasena: accion Desvincular subordinada (rol de error).
//  - vinculada sin contrasena (origen Google): solo estado, sin accion (es su unica credencial).
// Mientras la llamada esta en vuelo, el final muestra progreso (anunciado a lectores de pantalla).
@Composable
internal fun AccountGoogleRow(
    googleLinked: Boolean,
    canUnlink: Boolean,
    updating: Boolean,
    enabled: Boolean,
    onLink: () -> Unit,
    onUnlink: () -> Unit,
) {
    val updatingDesc = stringResource(R.string.account_google_updating)
    val body: @Composable () -> Unit = {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                // tint = Unspecified conserva los colores de marca del logotipo (no lo aplana a un tono).
                Icon(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (googleLinked) R.string.account_google_linked else R.string.account_link_google,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(
                        if (googleLinked) R.string.account_google_linked_desc else R.string.account_link_google_desc,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                updating -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier
                        .size(20.dp)
                        .semantics {
                            contentDescription = updatingDesc
                            liveRegion = LiveRegionMode.Polite
                        },
                )
                !googleLinked -> Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                canUnlink -> TextButton(
                    onClick = onUnlink,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(stringResource(R.string.account_unlink_google)) }
            }
        }
    }

    if (!googleLinked) {
        Surface(
            onClick = onLink,
            enabled = enabled,
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) { body() }
    } else {
        Surface(
            shape = CardShape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) { body() }
    }
}

@Composable
internal fun AccountHeader(session: Session?) {
    val rawName = session?.user?.nombre
    val initials = remember(rawName) { initialsOf(rawName) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            // Iniciales si hay nombre real; el icono Person es el reemplazo cuando aun no hay sesion.
            if (initials.isNotEmpty()) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Spacer(Modifier.size(Spacing.lg))
        Column {
            Text(
                text = rawName?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.account_default_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            val email = session?.user?.email
            if (!email.isNullOrBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// Iniciales del nombre: primera + ultima palabra (o solo la primera). Vacio si el nombre esta en
// blanco, para que la cabecera caiga al icono Person.
private fun initialsOf(name: String?): String {
    val parts = name?.trim()?.split(Regex("\\s+"))?.filter { it.isNotBlank() }.orEmpty()
    return when {
        parts.isEmpty() -> ""
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}
