package com.eddndev.purpura.ui.verification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.CardShape
import com.eddndev.purpura.ui.theme.Spacing

// Aviso "verifica tu correo" en Inicio. Autocontenido: muestra titulo+cuerpo, una accion (Enviar /
// Reenviar) con su propio progreso, y el error inline si el envio falla. No se pinta si no toca
// (state.visible=false). El copy cambia tras enviar para invitar a revisar la bandeja.
@Composable
fun EmailVerificationBanner(
    state: EmailVerificationUiState,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.visible) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screenH, vertical = Spacing.sm),
        shape = CardShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Icon(
                imageVector = if (state.sent) Icons.Outlined.MarkEmailRead else Icons.Outlined.MarkEmailUnread,
                contentDescription = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (state.sent) R.string.account_verify_sent_title else R.string.account_verify_title,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(
                        if (state.sent) R.string.account_verify_sent_body else R.string.account_verify_body,
                        state.email,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                state.errorRes?.let { res ->
                    Text(
                        text = stringResource(res),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
            if (state.isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Spacing.xl),
                    strokeWidth = 2.dp,
                )
            } else {
                TextButton(onClick = onSend) {
                    Text(
                        stringResource(
                            if (state.sent) R.string.account_verify_resend else R.string.account_verify_action,
                        ),
                    )
                }
            }
        }
    }
}
