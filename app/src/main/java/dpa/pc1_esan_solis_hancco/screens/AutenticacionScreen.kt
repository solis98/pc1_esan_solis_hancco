package dpa.pc1_esan_solis_hancco.screens

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import dpa.pc1_esan_solis_hancco.data.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AutenticacionScreen(
    registro: Boolean,
    onCambiarPantalla: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (registro) "Crear cuenta" else "Iniciar sesión",
            style = MaterialTheme.typography.headlineMedium
        )

        Text("Lotería · Sorteo de prueba")

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                error = null
            },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email
            ),
            enabled = !cargando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            enabled = !cargando,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (registro) {
            OutlinedTextField(
                value = confirmacion,
                onValueChange = {
                    confirmacion = it
                    error = null
                },
                label = { Text("Confirmar contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                enabled = !cargando,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            enabled = !cargando,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                error = when {
                    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                        "Ingresa un correo válido."

                    password.isEmpty() ->
                        "Ingresa la contraseña."

                    registro && password.length < 6 ->
                        "La contraseña debe tener al menos seis caracteres."

                    registro && password != confirmacion ->
                        "Las contraseñas no coinciden."

                    else -> null
                }

                if (error == null) {
                    cargando = true

                    scope.launch {
                        try {
                            if (registro) {
                                AuthRepository.registrar(email, password)
                            } else {
                                AuthRepository.iniciarSesion(email, password)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = when (e) {
                                is FirebaseNetworkException ->
                                    "Revisa la conexión a internet."

                                is FirebaseAuthUserCollisionException ->
                                    "Este correo ya tiene una cuenta. Inicia sesión."

                                is FirebaseAuthWeakPasswordException ->
                                    "La contraseña no cumple los requisitos de Firebase."

                                is FirebaseAuthInvalidCredentialsException,
                                is FirebaseAuthInvalidUserException ->
                                    "Revisa el correo y la contraseña."

                                else ->
                                    "No se pudo completar la operación. Inténtalo nuevamente."
                            }
                        } finally {
                            cargando = false
                        }
                    }
                }
            }
        ) {
            Text(
                when {
                    cargando -> "Procesando..."
                    registro -> "Crear cuenta"
                    else -> "Iniciar sesión"
                }
            )
        }

        TextButton(
            enabled = !cargando,
            onClick = onCambiarPantalla,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (registro) "Ya tengo cuenta: iniciar sesión"
                else "No tengo cuenta: registrarme"
            )
        }
    }
}