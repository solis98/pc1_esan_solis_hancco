package dpa.pc1_esan_solis_hancco.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dpa.pc1_esan_solis_hancco.data.LoteriaRepository
import dpa.pc1_esan_solis_hancco.model.Jugada
import dpa.pc1_esan_solis_hancco.model.Sorteo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun JugadaScreen(
    uid: String,
    onVerSeguimiento: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var sorteo by remember { mutableStateOf<Sorteo?>(null) }
    var jugada by remember { mutableStateOf<Jugada?>(null) }

    var sorteoCargado by remember { mutableStateOf(false) }
    var jugadaCargada by remember { mutableStateOf(false) }

    var errorSorteo by remember { mutableStateOf<String?>(null) }
    var errorJugada by remember { mutableStateOf<String?>(null) }
    var errorFormulario by remember { mutableStateOf<String?>(null) }

    var numerosTexto by rememberSaveable { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf("") }
    var intento by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    DisposableEffect(uid, intento) {
        sorteoCargado = false
        jugadaCargada = false
        errorSorteo = null
        errorJugada = null

        val listenerSorteo = LoteriaRepository.escucharSorteo(
            onDatos = {
                sorteo = it
                sorteoCargado = true
                errorSorteo = null
            },
            onError = {
                errorSorteo = it
                sorteoCargado = true
            }
        )

        val listenerJugada = LoteriaRepository.escucharJugada(
            uid = uid,
            onDatos = {
                jugada = it
                jugadaCargada = true
                errorJugada = null
            },
            onError = {
                errorJugada = it
                jugadaCargada = true
            }
        )

        onDispose {
            listenerSorteo.remove()
            listenerJugada.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Mi jugada",
            style = MaterialTheme.typography.headlineMedium
        )

        val sorteoActual = sorteo
        val jugadaActual = jugada

        if (sorteoActual != null) {
            Text("Sorteo: ${sorteoActual.nombre}")
            Text("Fecha: ${formatoFecha.format(sorteoActual.fecha)}")
            Text("Estado: ${sorteoActual.estado}")
        }

        when {
            !sorteoCargado || !jugadaCargada -> {
                Text("Cargando datos...")
            }

            errorSorteo != null || errorJugada != null -> {
                Text(
                    text = errorSorteo ?: errorJugada.orEmpty(),
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = { intento++ }) {
                    Text("Reintentar")
                }
            }

            jugadaActual != null -> {
                Text(
                    text = "Tus números: ${
                        jugadaActual.numeros.joinToString(", ")
                    }",
                    style = MaterialTheme.typography.titleMedium
                )

                Text("Tu jugada está registrada y no puede modificarse.")

                Button(
                    onClick = onVerSeguimiento,
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver seguimiento")
                }
            }

            sorteoActual?.estado == "abierto" -> {
                Text("Ingresa seis números distintos entre 1 y 45.")

                OutlinedTextField(
                    value = numerosTexto,
                    onValueChange = {
                        numerosTexto = it
                        errorFormulario = null
                    },
                    label = { Text("Seis números separados por comas") },
                    placeholder = { Text("5, 12, 18, 23, 34, 41") },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Revisa tu selección: después de guardarla no se edita.")

                Button(
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        errorFormulario = null
                        mensaje = ""

                        val numeros = numerosTexto
                            .split(",")
                            .map { it.trim().toIntOrNull() }

                        if (
                            numeros.size != 6 ||
                            numeros.any { it == null }
                        ) {
                            errorFormulario =
                                "Ingresa exactamente seis números enteros."
                        } else {
                            val seleccion = numeros.filterNotNull()

                            when {
                                seleccion.any { it !in 1..45 } -> {
                                    errorFormulario =
                                        "Los números deben estar entre 1 y 45."
                                }

                                seleccion.distinct().size != 6 -> {
                                    errorFormulario =
                                        "Los números no pueden repetirse."
                                }

                                else -> {
                                    guardando = true

                                    scope.launch {
                                        try {
                                            LoteriaRepository.registrarJugada(
                                                uid = uid,
                                                numeros = seleccion
                                            )

                                            jugada = Jugada(
                                                uid = uid,
                                                sorteoId = LoteriaRepository.SORTEO_ID,
                                                numeros = seleccion
                                            )

                                            mensaje = "Jugada registrada."
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            errorFormulario =
                                                "No se pudo guardar: ${e.message}"
                                        } finally {
                                            guardando = false
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        if (guardando) "Guardando..."
                        else "Registrar jugada"
                    )
                }
            }

            else -> {
                Text(
                    "El sorteo ya comenzó o finalizó. " +
                            "No puedes registrar una jugada."
                )
            }
        }

        errorFormulario?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (mensaje.isNotEmpty()) {
            Text(mensaje)
        }

        OutlinedButton(
            onClick = onCerrarSesion,
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}