package br.com.canarinho.redesocial.dao

import android.content.Context
import br.com.canarinho.redesocial.model.Post
import br.com.canarinho.redesocial.model.User
import br.com.canarinho.redesocial.util.Base64Converter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class PostDAO(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val collection = "posts"
    private val limitePorPagina = 5L

    var temMaisPosts: Boolean = true
        private set
    var carregando: Boolean = false
        private set
    private var ultimoTimestamp: Timestamp? = null

    fun resetarPaginacao() {
        ultimoTimestamp = null
        temMaisPosts = true
        carregando = false
    }

    fun getPaginado(
        onSuccess: (List<Post>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (carregando) return
        carregando = true

        var query = db.collection(collection)
            .orderBy("data", Query.Direction.DESCENDING)
            .limit(limitePorPagina)

        ultimoTimestamp?.let { query = query.startAfter(it) }

        query.get()
            .addOnSuccessListener { documentos ->
                temMaisPosts = documentos.size() >= limitePorPagina

                if (!documentos.isEmpty) {
                    ultimoTimestamp = documentos.documents.last().getTimestamp("data")
                }

                val emails = documentos.documents
                    .map { it.getString("emailAutor") ?: "" }
                    .filter { it.isNotEmpty() }
                    .distinct()

                if (emails.isEmpty()) {
                    carregando = false
                    onSuccess(emptyList())
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

                        val posts = mutableListOf<Post>()
                        for (document in documentos.documents) {
                            val id = document.id
                            val imageString = document.getString("imageString") ?: ""
                            val descricao = document.getString("descricao") ?: ""
                            val emailAutor = document.getString("emailAutor") ?: ""
                            val data = document.getTimestamp("data") ?: Timestamp.now()
                            val latitude = document.getDouble("latitude") ?: 0.0
                            val longitude = document.getDouble("longitude") ?: 0.0
                            val cidade = document.getString("cidade") ?: ""
                            val usuario = mapaUsuarios[emailAutor]
                            val usernameAutor = usuario?.username ?: emailAutor
                            val fotoAutor = usuario?.fotoPerfil?.let {
                                try { Base64Converter.stringToBitmap(it) } catch (e: Exception) { null }
                            }
                            val bitmap = try {
                                Base64Converter.stringToBitmap(imageString)
                            } catch (e: Exception) { null }

                            posts.add(Post(id, descricao, bitmap, emailAutor, usernameAutor, fotoAutor, data, latitude, longitude, cidade))
                        }

                        carregando = false
                        onSuccess(posts)
                    }
                    .addOnFailureListener {
                        carregando = false
                        onFailure(it.message ?: "Erro ao buscar autores")
                    }
            }
            .addOnFailureListener {
                carregando = false
                onFailure(it.message ?: "Erro ao carregar posts")
            }
    }

    fun save(
        descricao: String,
        imageString: String,
        emailAutor: String,
        latitude: Double,
        longitude: Double,
        cidade: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val post = hashMapOf(
            "descricao" to descricao,
            "imageString" to imageString,
            "emailAutor" to emailAutor,
            "data" to Timestamp.now(),
            "latitude" to latitude,
            "longitude" to longitude,
            "cidade" to cidade
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