package dpa.pc1_esan_solis_hancco.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import dpa.pc1_esan_solis_hancco.data.AuthRepository
import dpa.pc1_esan_solis_hancco.screens.AutenticacionScreen
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import dpa.pc1_esan_solis_hancco.screens.JugadaScreen

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val auth = AuthRepository.auth
    var usuario by remember { mutableStateOf(auth.currentUser) }
    var registro by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            usuario = it.currentUser
        }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        val usuarioActual = usuario
        val context = LocalContext.current

        if (usuarioActual == null) {
            key(registro) {
                AutenticacionScreen(
                    registro = registro,
                    onCambiarPantalla = {
                        registro = !registro
                    }
                )
            }
        } else {
            key(usuarioActual.uid) {
                Column {
                    Text(
                        text = "Participante: ${usuarioActual.email}",
                        modifier = Modifier.padding(16.dp)
                    )

                    JugadaScreen(
                        uid = usuarioActual.uid,
                        onVerSeguimiento = {
                            Toast.makeText(
                                context,
                                "El seguimiento se conectará en el siguiente paso.",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onCerrarSesion = {
                            registro = false
                            AuthRepository.cerrarSesion()
                        }
                    )
                }
            }
        }
    }
}