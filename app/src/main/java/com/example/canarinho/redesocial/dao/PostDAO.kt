package br.com.canarinho.redesocial.dao

import android.content.Context
import br.com.canarinho.redesocial.model.Post
import br.com.canarinho.redesocial.util.Base64Converter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostDAO(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "posts"
    private val limitePorPagina = 5L

    // Cursor da paginação — guarda o Timestamp do último post carregado
    private var ultimoTimestamp: Timestamp? = null
    // Controla se ainda há mais posts para carregar
    var temMaisPosts: Boolean = true
        private set

    fun resetarPaginacao() {
        ultimoTimestamp = null
        temMaisPosts = true
    }

    fun getPaginado(
        onSuccess: (List<Post>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        var query = db.collection(collection)
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(limitePorPagina)

        // Se já carregamos alguma página, começa após o último Timestamp
        ultimoTimestamp?.let {
            query = query.startAfter(it)
        }

        query.get()
            .addOnSuccessListener { documentos ->
                // Se veio menos que o limite, não há mais páginas
                temMaisPosts = documentos.size() >= limitePorPagina

                if (!documentos.isEmpty) {
                    // Salva o Timestamp do último documento como cursor
                    ultimoTimestamp = documentos.documents.last().getTimestamp("data")
                }

                val posts = mutableListOf<Post>()
                for (document in documentos.documents) {
                    val id = document.id
                    val imageString = document.data?.get("imageString")?.toString() ?: ""
                    val descricao = document.data?.get("descricao")?.toString() ?: ""
                    val emailAutor = document.data?.get("emailAutor")?.toString() ?: ""
                    val data = document.getTimestamp("data") ?: Timestamp.now()
                    val bitmap = try {
                        Base64Converter.stringToBitmap(imageString)
                    } catch (e: Exception) {
                        null
                    }
                    posts.add(Post(id, descricao, bitmap, emailAutor, data))
                }
                onSuccess(posts)
            }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao carregar posts") }
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
            "emailAutor" to emailAutor,
            "data" to Timestamp.now()
        )
        db.collection(collection).add(post)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao salvar post") }
    }

    fun update(
        postId: String,
        descricao: String,
        imageString: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = hashMapOf<String, Any>(
            "descricao" to descricao,
            "imageString" to imageString
        )
        db.collection(collection).document(postId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao editar post") }
    }

    fun delete(postId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection(collection).document(postId).delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Erro ao deletar post") }
    }
}