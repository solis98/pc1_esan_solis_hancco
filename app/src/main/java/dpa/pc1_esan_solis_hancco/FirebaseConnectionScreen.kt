package dpa.pc1_esan_solis_hancco

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun FirebaseConnectionScreen(
    modifier: Modifier = Modifier
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val firestore = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    var usuario by remember { mutableStateOf(auth.currentUser) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ingresando by remember { mutableStateOf(false) }
    var leyendo by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var intento by remember { mutableStateOf(0) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            usuario = it.currentUser
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(usuario?.uid, intento) {
        mensaje = ""
        error = null
        leyendo = false

        if (usuario != null) {
            password = ""
            leyendo = true

            try {
                val documento = firestore
                    .collection("connection_test")
                    .document("config")
                    .get(Source.SERVER)
                    .await()

                mensaje = documento.getString("message")
                    ?: throw Exception(
                        "No existe connection_test/config " +
                                "o falta el campo message."
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Error al leer Firestore: ${e.message}"
            } finally {
                leyendo = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Prueba de conexión Firebase",
            style = MaterialTheme.typography.headlineSmall
        )

        if (usuario == null) {
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
                enabled = !ingresando,
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
                enabled = !ingresando,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = !ingresando,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (email.isBlank() || password.isEmpty()) {
                        error = "Completa el correo y la contraseña."
                    } else {
                        ingresando = true
                        error = null

                        scope.launch {
                            try {
                                auth.signInWithEmailAndPassword(
                                    email.trim(),
                                    password
                                ).await()
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = "No se pudo iniciar sesión: ${e.message}"
                            } finally {
                                ingresando = false
                            }
                        }
                    }
                }
            ) {
                Text(
                    if (ingresando) "Ingresando..."
                    else "Iniciar sesión"
                )
            }
        } else {
            Text("Authentication: sesión iniciada")
            Text("Usuario: ${usuario?.email}")

            if (leyendo) {
                Text("Consultando Firestore...")
            }

            if (mensaje.isNotEmpty()) {
                Text("Firestore: $mensaje")
            }

            Button(
                enabled = !leyendo,
                onClick = { intento++ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Volver a comprobar Firestore")
            }

            Button(
                onClick = {
                    mensaje = ""
                    error = null
                    auth.signOut()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }

        error?.let { texto ->
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}