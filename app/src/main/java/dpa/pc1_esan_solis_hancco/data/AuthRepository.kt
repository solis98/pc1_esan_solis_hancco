package dpa.pc1_esan_solis_hancco.data

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AuthRepository {
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    suspend fun iniciarSesion(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    suspend fun registrar(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email.trim(), password).await()
    }

    fun cerrarSesion() {
        auth.signOut()
    }
}