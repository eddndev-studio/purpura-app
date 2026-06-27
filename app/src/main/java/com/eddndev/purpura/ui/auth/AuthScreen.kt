package com.eddndev.purpura.ui.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.eddndev.purpura.R
import com.eddndev.purpura.ui.compose.LoadingButton
import com.eddndev.purpura.ui.theme.Pill
import com.eddndev.purpura.ui.theme.Spacing
import kotlinx.coroutines.launch

/**
 * Autenticacion (06-app-architecture §13.1) en Compose: formulario centrado de inicio de sesion /
 * registro con correo y contrasena, o Google Sign-In. SIN chrome (Scaffold propio sin TopAppBar): es
 * la puerta de entrada, no una pantalla del bottom nav, por eso NO usa PurpuraScreen. El modo
 * login/registro vive en estado local (rememberSaveable); el campo Nombre solo aparece en registro.
 * submit valida campos no vacios y formato de correo antes de delegar en [onLogin] / [onRegister]. Los
 * errores del ViewModel y la validacion local se muestran como snackbar; el formato de correo se marca
 * inline en el campo. Esta pantalla NO navega: al persistir la sesion, MainActivity navega a Inicio.
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

    // Contrasena: el backend exige >= 8 (auth_service.go minPasswordLen). Solo en REGISTRO marcamos el
    // campo en rojo cuando ya hay texto pero queda corto; en login no prejuzgamos (la credencial la
    // valida el servidor). El hint "Minimo 8 caracteres" se muestra siempre en registro como guia.
    val passwordTooShort = isRegister && password.isNotEmpty() && password.length < MIN_PASSWORD_LENGTH

    // stringResource debe resolverse en scope composable, no dentro del click.
    val emptyFieldsMessage = stringResource(R.string.auth_error_empty_fields)
    val invalidEmailMessage = stringResource(R.string.auth_error_invalid_email)
    val passwordHintMessage = stringResource(R.string.auth_password_hint)

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
        // Solo en registro: corta antes de pegarle al backend y avisa con el mismo hint del campo.
        if (isRegister && password.length < MIN_PASSWORD_LENGTH) {
            scope.launch {
                snackbarHostState.showSnackbar(passwordHintMessage, duration = SnackbarDuration.Short)
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
            // Branding -> formulario: salto de seccion para separar marca de la tarea (Spacing.section).
            Spacer(Modifier.height(Spacing.section))

            // Campo Nombre: aparece solo en registro con revelado suave (expand + fade, 200ms).
            AnimatedVisibility(
                visible = isRegister,
                enter = expandVertically(tween(200)) + fadeIn(tween(200)),
            ) {
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
                // Slot de soporte SIEMPRE presente (reserva una linea) para que el campo no salte
                // al aparecer/desaparecer la pista de correo invalido.
                supportingText = {
                    Text(if (emailError) invalidEmailMessage else "")
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
                // Tope de 72 para no exceder el maximo del backend (maxPasswordLen) en el caso comun
                // ASCII; el servidor sigue siendo la autoridad sobre el limite real (en bytes).
                onValueChange = { if (it.length <= MAX_PASSWORD_LENGTH) password = it },
                label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true,
                enabled = !loading,
                isError = passwordTooShort,
                // Slot de soporte SIEMPRE presente (como el campo de correo) para que el campo no salte
                // al alternar login/registro: muestra el hint en registro y queda vacio en login.
                supportingText = { Text(if (isRegister) passwordHintMessage else "") },
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

            // CTA primario: progreso EN LINEA via LoadingButton (sin spinner separado que mueva el
            // layout). Alto >= 56dp y ancho completo para anclar la jerarquia de la pantalla.
            LoadingButton(
                onClick = { submit() },
                text = stringResource(if (isRegister) R.string.auth_register else R.string.auth_login),
                isLoading = loading,
                enabled = !loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            )

            // Toggle login/registro demotado a fila inline (jerarquia bajo el CTA).
            AuthModeToggle(
                isRegister = isRegister,
                enabled = !loading,
                onToggle = { isRegister = !isRegister },
            )

            Spacer(Modifier.height(Spacing.lg))
            AuthDivider()
            Spacer(Modifier.height(Spacing.lg))

            AuthGoogleButton(onClick = onGoogleClick, enabled = !loading)
        }
    }
}

// Reflejan la politica del backend (auth_service.go: minPasswordLen = 8, maxPasswordLen = 72) para dar
// feedback inmediato sin viaje de red. El backend mide en BYTES; aqui en caracteres como guarda
// practica del caso comun (el servidor es la autoridad final). Mantener en sincronia con el backend.
private const val MIN_PASSWORD_LENGTH = 8
private const val MAX_PASSWORD_LENGTH = 72
