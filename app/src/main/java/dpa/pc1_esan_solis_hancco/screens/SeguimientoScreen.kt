package dpa.pc1_esan_solis_hancco.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dpa.pc1_esan_solis_hancco.data.LoteriaRepository
import dpa.pc1_esan_solis_hancco.model.Bolilla
import dpa.pc1_esan_solis_hancco.model.Jugada
import dpa.pc1_esan_solis_hancco.model.Sorteo
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SeguimientoScreen(
    uid: String,
    onVolver: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var sorteo by remember { mutableStateOf<Sorteo?>(null) }
    var jugada by remember { mutableStateOf<Jugada?>(null) }
    var bolillas by remember { mutableStateOf<List<Bolilla>>(emptyList()) }

    var sorteoCargado by remember { mutableStateOf(false) }
    var jugadaCargada by remember { mutableStateOf(false) }
    var bolillasCargadas by remember { mutableStateOf(false) }

    var errorSorteo by remember { mutableStateOf<String?>(null) }
    var errorJugada by remember { mutableStateOf<String?>(null) }
    var errorBolillas by remember { mutableStateOf<String?>(null) }

    var intento by remember { mutableStateOf(0) }

    val formatoFecha = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

    DisposableEffect(uid, intento) {
        sorteoCargado = false
        jugadaCargada = false
        bolillasCargadas = false

        errorSorteo = null
        errorJugada = null
        errorBolillas = null

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

        val listenerBolillas = LoteriaRepository.escucharBolillas(
            onDatos = {
                bolillas = it
                bolillasCargadas = true
                errorBolillas = null
            },
            onError = {
                errorBolillas = it
                bolillasCargadas = true
            }
        )

        onDispose {
            listenerSorteo.remove()
            listenerJugada.remove()
            listenerBolillas.remove()
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
            text = "Seguimiento del sorteo",
            style = MaterialTheme.typography.headlineSmall
        )

        val error = errorSorteo ?: errorJugada ?: errorBolillas
        val sorteoActual = sorteo
        val jugadaActual = jugada

        when {
            !sorteoCargado || !jugadaCargada || !bolillasCargadas -> {
                Text("Cargando sorteo y jugada...")
            }

            error != null -> {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = { intento++ }) {
                    Text("Reintentar conexión")
                }
            }

            sorteoActual == null || jugadaActual == null -> {
                Text("No se encontró el sorteo o tu jugada.")
            }

            else -> {
                val sorteados = bolillas.map { it.numero }.toSet()
                val acertados = jugadaActual.numeros
                    .toSet()
                    .intersect(sorteados)

                val aciertos = acertados.size

                Text("Sorteo: ${sorteoActual.nombre}")
                Text("Fecha: ${formatoFecha.format(sorteoActual.fecha)}")

                Text(
                    text = "Tu jugada",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    jugadaActual.numeros.forEach { numero ->
                        val acertado = numero in acertados

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small,
                            color = if (acertado) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (acertado) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Text(
                                text = numero.toString(),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        }
                    }
                }

                Text("Los números resaltados son tus aciertos.")

                Text(
                    text = "Bolillas en orden de salida",
                    style = MaterialTheme.typography.titleMedium
                )

                if (bolillas.isEmpty()) {
                    Text("Todavía no se publicaron bolillas.")
                } else {
                    Text(
                        text = bolillas.joinToString(" → ") { it.numero.toString() },
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Text("Bolillas publicadas: ${bolillas.size}/6")
                Text("Aciertos: $aciertos/6")

                val resumen = when (sorteoActual.estado) {
                    "abierto" -> "Esperando el inicio del sorteo."

                    "en curso" -> "Sorteo en curso"

                    "finalizado" -> {
                        if (bolillas.size < 6) {
                            "Esperando resultados: faltan bolillas."
                        } else if (aciertos == 6) {
                            "GANADOR"
                        } else {
                            "SIGUE INTENTANDO"
                        }
                    }

                    else -> "Estado desconocido."
                }

                Text(
                    text = resumen,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver a mi jugada")
        }

        OutlinedButton(
            onClick = onCerrarSesion,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar sesión")
        }
    }
}