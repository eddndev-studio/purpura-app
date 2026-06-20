package com.eddndev.purpura.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.Pill
import kotlinx.coroutines.launch

/**
 * Autenticacion (06-app-architecture §13.1) en Compose: formulario centrado de inicio de sesion /
 * registro con correo y contrasena, o Google Sign-In. El modo login/registro vive en estado local
 * (rememberSaveable); el campo Nombre solo aparece en registro. submit valida campos no vacios antes
 * de delegar en [onLogin] / [onRegister]. Los errores del ViewModel y la validacion local se muestran
 * como snackbar. Esta pantalla NO navega: al persistir la sesion, MainActivity navega a Inicio.
 */
@Composable
fun AuthScreen(
    state: AuthUiState,
    onLogin: (email: String, password: String) -> Unit,
    onRegister: (email: String, name: String, password: String) -> Unit,
    onGoogleClick: () -> Unit,
    onErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val loading = state is AuthUiState.Loading

    // Estado local del formulario: sobrevive a rotacion (rememberSaveable).
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Mensaje de validacion local: stringResource debe resolverse en scope composable, no en el click.
    val emptyFieldsMessage = stringResource(R.string.auth_error_empty_fields)

    // Error del ViewModel -> snackbar de un solo uso (igual que HomeScreen).
    if (state is AuthUiState.Error) {
        val message = stringResource(state.messageRes)
        LaunchedEffect(state) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            onErrorShown()
        }
    }

    // Valida campos no vacios (correo y nombre sin espacios alrededor; la contrasena no se recorta).
    fun submit() {
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty() || (isRegister && trimmedName.isEmpty())) {
            scope.launch {
                snackbarHostState.showSnackbar(emptyFieldsMessage, duration = SnackbarDuration.Short)
            }
            return
        }
        if (isRegister) onRegister(trimmedEmail, trimmedName, password) else onLogin(trimmedEmail, password)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icono de cabecera (mismo recurso y tinte morado que el layout XML).
            Icon(
                painter = painterResource(R.drawable.ic_calendar_month),
                contentDescription = stringResource(R.string.placeholder_icon_desc),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.title_auth),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.auth_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Campo Nombre: visible solo en modo registro (motion de aparicion).
            AnimatedVisibility(visible = isRegister) {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.auth_name)) },
                        singleLine = true,
                        shape = Pill,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                shape = Pill,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                shape = Pill,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        val icon = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                        val descRes = if (passwordVisible) {
                            R.string.auth_hide_password
                        } else {
                            R.string.auth_show_password
                        }
                        Icon(imageVector = icon, contentDescription = stringResource(descRes))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))

            // Boton principal: Iniciar sesion / Registrar segun el modo.
            Button(
                onClick = { submit() },
                enabled = !loading,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (isRegister) R.string.auth_register else R.string.auth_login))
            }

            // Alterna login/registro (igual que toggleMode del Fragment original).
            TextButton(
                onClick = { isRegister = !isRegister },
                enabled = !loading,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (isRegister) R.string.auth_toggle_to_login else R.string.auth_toggle_to_register,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_or),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            // Boton de Google (outline). Lanza el intent via callback del Fragment.
            OutlinedButton(
                onClick = onGoogleClick,
                enabled = !loading,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.auth_google))
            }

            // Spinner mientras se autentica (motion entre formulario y carga).
            AnimatedVisibility(visible = loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(16.dp))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
