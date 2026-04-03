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
                        val imageString = document.data?.get("imageString")?.toString() ?: ""
                        val descricao = document.data?.get("descricao")?.toString() ?: ""
                        val bitmap = try {
                            Base64Converter.stringToBitmap(imageString)
                        } catch (e: Exception) {
                            null
                        }
                        posts.add(Post(descricao, bitmap))
                    }
                    onSuccess(posts)
                } else {
                    onFailure(task.exception?.message ?: "Erro ao carregar posts")
                }
            }
    }
}