package dpa.pc1_esan_solis_hancco.model

import java.util.Date

data class Sorteo(
    val id: String,
    val nombre: String,
    val fecha: Date,
    val estado: String
)