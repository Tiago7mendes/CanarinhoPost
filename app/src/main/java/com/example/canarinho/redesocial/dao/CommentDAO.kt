package br.com.canarinho.redesocial.dao

import android.content.Context
import br.com.canarinho.redesocial.model.Comment
import br.com.canarinho.redesocial.model.User
import br.com.canarinho.redesocial.util.Base64Converter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class CommentDAO(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()

    // Comentários ficam numa subcollection dentro de cada post
    private fun subcollection(postId: String) =
        db.collection("posts").document(postId).collection("comentarios")

    fun getAll(
        postId: String,
        onSuccess: (List<Comment>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        subcollection(postId)
            .orderBy("data", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documentos ->
                val comments = mutableListOf<Comment>()

                val emails = documentos.documents
                    .map { it.getString("emailAutor") ?: "" }
                    .filter { it.isNotEmpty() }
                    .distinct()

                if (emails.isEmpty()) {
                    onSuccess(comments)
                    return@addOnSuccessListener
                }

                db.collection("usuarios")
                    .whereIn("email", emails)
                    .get()
                    .addOnSuccessListener { usuarios ->
                        val mapaUsuarios = mutableMapOf<String, User>()
                        for (u in usuarios.documents) {
                            val user = u.toObject(User::class.java)
                            if (user != null) mapaUsuarios[user.email] = user
                        }

                        for (doc in documentos.documents) {
                            val emailAutor = doc.getString("emailAutor") ?: ""
                            val usuario = mapaUsuarios[emailAutor]
                            val fotoAutor = usuario?.fotoPerfil?.let {
                                try { Base64Converter.stringToBitmap(it) } catch (e: Exception) { null }
                            }
                            comments.add(
                                Comment(
                                    id = doc.id,
                                    texto = doc.getString("texto") ?: "",
                                    emailAutor = emailAutor,
                                    usernameAutor = usuario?.username ?: emailAutor,
                                    fotoAutor = fotoAutor,
                                    data = doc.getTimestamp("data") ?: Timestamp.now()
                                )
                            )
                        }
                        onSuccess(comments)
                    }
                    .addOnFailureListener { onFailure(it.message ?: "Erro ao buscar usuários") }
            }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao carregar comentários") }
    }

    fun save(
        postId: String,
        texto: String,
        emailAutor: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val comment = hashMapOf(
            "texto" to texto,
            "emailAutor" to emailAutor,
            "data" to Timestamp.now()
        )
        subcollection(postId).add(comment)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao salvar comentário") }
    }
}