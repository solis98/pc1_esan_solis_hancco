package dpa.pc1_esan_solis_hancco.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dpa.pc1_esan_solis_hancco.model.Jugada
import dpa.pc1_esan_solis_hancco.model.Sorteo
import kotlinx.coroutines.tasks.await

object LoteriaRepository {
    const val SORTEO_ID = "prueba"

    private val db = FirebaseFirestore.getInstance()

    private fun sorteoRef() =
        db.collection("sorteos").document(SORTEO_ID)

    fun escucharSorteo(
        onDatos: (Sorteo) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return sorteoRef().addSnapshotListener { documento, fallo ->
            if (fallo != null) {
                onError(fallo.message ?: "No se pudo leer el sorteo.")
                return@addSnapshotListener
            }

            try {
                require(documento != null && documento.exists()) {
                    "No existe sorteos/$SORTEO_ID."
                }

                val nombre = documento.getString("nombre")
                    ?: throw Exception("Falta el nombre del sorteo.")

                val fecha = documento.getTimestamp("fecha")?.toDate()
                    ?: throw Exception("Falta una fecha de tipo timestamp.")

                val estado = documento.getString("estado")
                    ?: throw Exception("Falta el estado.")

                require(estado in listOf("abierto", "en curso", "finalizado")) {
                    "Estado del sorteo incorrecto."
                }

                onDatos(
                    Sorteo(
                        id = documento.id,
                        nombre = nombre,
                        fecha = fecha,
                        estado = estado
                    )
                )
            } catch (e: Exception) {
                onError(e.message ?: "Datos del sorteo incorrectos.")
            }
        }
    }

    fun escucharJugada(
        uid: String,
        onDatos: (Jugada?) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration {
        return sorteoRef()
            .collection("jugadas")
            .document(uid)
            .addSnapshotListener { documento, fallo ->
                if (fallo != null) {
                    onError(fallo.message ?: "No se pudo leer la jugada.")
                    return@addSnapshotListener
                }

                try {
                    require(documento != null) {
                        "No se recibió el documento."
                    }

                    if (!documento.exists()) {
                        onDatos(null)
                    } else {
                        val lista = documento.get("numeros") as? List<*>
                            ?: throw Exception("Falta la lista de números.")

                        val numeros = lista.map {
                            val numero = it as? Long
                                ?: throw Exception("Los números deben ser enteros.")

                            require(numero in 1L..45L) {
                                "Número fuera del rango de 1 a 45."
                            }

                            numero.toInt()
                        }

                        require(
                            numeros.size == 6 &&
                                    numeros.distinct().size == 6
                        ) {
                            "La jugada debe tener seis números distintos."
                        }

                        require(
                            documento.getString("uid") == uid &&
                                    documento.getString("sorteoId") == SORTEO_ID
                        ) {
                            "La jugada no corresponde al usuario o sorteo."
                        }

                        onDatos(
                            Jugada(
                                uid = uid,
                                sorteoId = SORTEO_ID,
                                numeros = numeros
                            )
                        )
                    }
                } catch (e: Exception) {
                    onError(e.message ?: "Datos de la jugada incorrectos.")
                }
            }
    }

    suspend fun registrarJugada(
        uid: String,
        numeros: List<Int>
    ) {
        require(FirebaseAuth.getInstance().currentUser?.uid == uid) {
            "Debes iniciar sesión con este usuario."
        }

        require(
            numeros.size == 6 &&
                    numeros.distinct().size == 6 &&
                    numeros.all { it in 1..45 }
        ) {
            "Selecciona seis enteros distintos entre 1 y 45."
        }

        val sorteo = sorteoRef()
        val jugada = sorteo.collection("jugadas").document(uid)

        db.runTransaction { transaction ->
            val datosSorteo = transaction.get(sorteo)
            val datosJugada = transaction.get(jugada)

            check(datosSorteo.getString("estado") == "abierto") {
                "El sorteo ya no está abierto."
            }

            check(!datosJugada.exists()) {
                "Ya registraste una jugada para este sorteo."
            }

            transaction.set(
                jugada,
                mapOf(
                    "uid" to uid,
                    "sorteoId" to SORTEO_ID,
                    "numeros" to numeros
                )
            )

            Unit
        }.await()
    }
}