package br.com.canarinho.redesocial.dao

import android.content.Context
import br.com.canarinho.redesocial.model.User
import com.google.firebase.firestore.FirebaseFirestore

class UserDAO(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "usuarios"

    fun save(user: User, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection(collection).document(user.email)
            .set(user)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro desconhecido") }
    }

    fun getByEmail(email: String, onSuccess: (User?) -> Unit, onFailure: (String) -> Unit) {
        db.collection(collection).document(email).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val user = snapshot.toObject(User::class.java)
                    onSuccess(user)
                } else {
                    onSuccess(null)
                }
            }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao carregar") }
    }
}