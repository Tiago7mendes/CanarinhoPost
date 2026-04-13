package br.com.canarinho.redesocial.dao

import android.content.Context
import br.com.canarinho.redesocial.model.Post
import br.com.canarinho.redesocial.util.Base64Converter
import com.google.firebase.firestore.FirebaseFirestore

class PostDAO(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "posts"

    fun getAll(onSuccess: (List<Post>) -> Unit, onFailure: (String) -> Unit) {
        db.collection(collection).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val posts = mutableListOf<Post>()
                    for (document in task.result.documents) {
                        val id = document.id
                        val imageString = document.data?.get("imageString")?.toString() ?: ""
                        val descricao = document.data?.get("descricao")?.toString() ?: ""
                        val emailAutor = document.data?.get("emailAutor")?.toString() ?: ""
                        val bitmap = try {
                            Base64Converter.stringToBitmap(imageString)
                        } catch (e: Exception) {
                            null
                        }
                        posts.add(Post(id, descricao, bitmap, emailAutor))
                    }
                    onSuccess(posts)
                } else {
                    onFailure(task.exception?.message ?: "Erro ao carregar posts")
                }
            }
    }

    fun save(
        descricao: String,
        imageString: String,
        emailAutor: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val post = hashMapOf(
            "descricao" to descricao,
            "imageString" to imageString,
            "emailAutor" to emailAutor
        )
        db.collection(collection).add(post)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao salvar post") }
    }

    fun delete(postId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection(collection).document(postId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao deletar post") }
    }
}