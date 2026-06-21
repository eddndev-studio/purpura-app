package com.eddndev.purpura.ui.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Autenticacion (06-app-architecture §13.1) en Compose: formulario centrado de inicio de sesion /
 * registro con correo y contrasena, o Google Sign-In. SIN chrome (sin TopAppBar): es la puerta de
 * entrada, no una pantalla del bottom nav. El modo login/registro vive en estado local
 * (rememberSaveable); el campo Nombre solo aparece en registro. submit valida campos no vacios y
 * formato de correo antes de delegar en [onLogin] / [onRegister]. Los errores del ViewModel y la
 * validacion local se muestran como snackbar; el formato de correo se marca inline en el campo.
 * Esta pantalla NO navega: al persistir la sesion, MainActivity navega a Inicio.
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
    val focusManager = LocalFocusManager.current
    val passwordFocus = remember { FocusRequester() }
    val loading = state is AuthUiState.Loading

    // Estado local del formulario: sobrevive a rotacion (rememberSaveable).
    var isRegister by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Solo marcamos el correo en rojo cuando hay texto y NO es valido (no en el primer render).
    val emailValid = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val emailError = email.isNotBlank() && !emailValid

    // stringResource debe resolverse en scope composable, no dentro del click.
    val emptyFieldsMessage = stringResource(R.string.auth_error_empty_fields)
    val invalidEmailMessage = stringResource(R.string.auth_error_invalid_email)

    // Error del ViewModel -> snackbar de un solo uso (igual que HomeScreen).
    if (state is AuthUiState.Error) {
        val message = stringResource(state.messageRes)
        LaunchedEffect(state) {
            snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Long)
            onErrorShown()
        }
    }

    // Valida campos no vacios y formato de correo antes de delegar al ViewModel.
    fun submit() {
        focusManager.clearFocus()
        val trimmedEmail = email.trim()
        val trimmedName = name.trim()
        if (trimmedEmail.isEmpty() || password.isEmpty() || (isRegister && trimmedName.isEmpty())) {
            scope.launch {
                snackbarHostState.showSnackbar(emptyFieldsMessage, duration = SnackbarDuration.Short)
            }
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            scope.launch {
                snackbarHostState.showSnackbar(invalidEmailMessage, duration = SnackbarDuration.Short)
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
                .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AuthBranding()
            Spacer(Modifier.height(Spacing.xl))

            // Campo Nombre: visible solo en modo registro (motion de aparicion suave).
            AnimatedVisibility(visible = isRegister) {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.auth_name)) },
                        singleLine = true,
                        enabled = !loading,
                        shape = Pill,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(Spacing.md))
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text(stringResource(R.string.auth_email)) },
                singleLine = true,
                enabled = !loading,
                isError = emailError,
                shape = Pill,
                // Pista inline solo cuando el correo escrito no tiene formato valido.
                supportingText = if (emailError) {
                    { Text(invalidEmailMessage) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.md))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                enabled = !loading,
                shape = Pill,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus),
            )
            Spacer(Modifier.height(Spacing.lg))

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

            Spacer(Modifier.height(Spacing.md))
            AuthDivider()
            Spacer(Modifier.height(Spacing.md))

            // Boton de Google (outline). Lanza el intent via callback del Fragment.
            // Nota: sin glifo de marca en res/drawable; usamos solo texto para no mostrar un
            // icono enganoso (ver risks: falta ic_google de la fundacion).
            OutlinedButton(
                onClick = onGoogleClick,
                enabled = !loading,
                shape = Pill,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.auth_google))
            }

            // Spinner mientras se autentica (motion entre formulario y carga); reserva alto fijo
            // para evitar que el contenido salte al aparecer/desaparecer.
            AnimatedVisibility(visible = loading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Spacing.xxl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(Spacing.lg))
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Branding centrado: icono de calendario en pastilla primaria + titulo y subtitulo de marca. */
@Composable
private fun AuthBranding() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, Pill),
            contentAlignment = Alignment.Center,
        ) {
            // Marca decorativa: el titulo "Bienvenido a Purpura" debajo ya describe la pantalla,
            // asi que el icono no necesita contentDescription (igual que los iconos del exemplar).
            Icon(
                painter = painterResource(R.drawable.ic_calendar_month),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp),
            )
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.title_auth),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = stringResource(R.string.auth_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Divisor "o" real: dos lineas con la etiqueta centrada (separa el formulario de Google). */
@Composable
private fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.auth_or),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
