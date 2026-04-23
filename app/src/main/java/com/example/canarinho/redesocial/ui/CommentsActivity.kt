package br.com.canarinho.redesocial.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.canarinho.redesocial.adapter.CommentAdapter
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.CommentDAO
import br.com.canarinho.redesocial.dao.UserDAO
import br.com.canarinho.redesocial.model.Comment
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityCommentsBinding
import com.google.firebase.Timestamp

class CommentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentsBinding
    private lateinit var commentDAO: CommentDAO
    private lateinit var userDAO: UserDAO
    private lateinit var adapter: CommentAdapter
    private val userAuth = UserAuth()
    private val comments = mutableListOf<Comment>()
    private var postId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postId = intent.getStringExtra("POST_ID") ?: ""
        commentDAO = CommentDAO(this)
        userDAO = UserDAO(this)

        setupAdapter()
        setupListeners()
        carregarComentarios()
    }

    private fun setupAdapter() {
        adapter = CommentAdapter(comments)
        binding.recyclerComentarios.layoutManager = LinearLayoutManager(this)
        binding.recyclerComentarios.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnVoltar.setOnClickListener { finish() }

        binding.btnEnviarComentario.setOnClickListener {
            enviarComentario()
        }
    }

    private fun carregarComentarios() {
        binding.progressBar.visibility = View.VISIBLE
        commentDAO.getAll(postId,
            onSuccess = { lista ->
                binding.progressBar.visibility = View.GONE
                comments.clear()
                comments.addAll(lista)
                adapter.notifyDataSetChanged()
            },
            onFailure = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun enviarComentario() {
        val texto = binding.edtComentario.text.toString().trim()
        val email = userAuth.getEmailUsuarioLogado() ?: return

        if (texto.isEmpty()) {
            Toast.makeText(this, "Escreva um comentário", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnEnviarComentario.isEnabled = false

        // Busca os dados do usuário logado para montar o comentário localmente
        userDAO.getByEmail(email,
            onSuccess = { user ->
                commentDAO.save(postId, texto, email,
                    onSuccess = {
                        val fotoAutor = user?.fotoPerfil?.let {
                            try { Base64Converter.stringToBitmap(it) } catch (e: Exception) { null }
                        }
                        val novoComment = Comment(
                            texto = texto,
                            emailAutor = email,
                            usernameAutor = user?.username ?: email,
                            fotoAutor = fotoAutor,
                            data = Timestamp.now()
                        )
                        adapter.adicionarComentario(novoComment)
                        binding.edtComentario.setText("")
                        binding.recyclerComentarios.scrollToPosition(comments.size - 1)
                        binding.btnEnviarComentario.isEnabled = true
                    },
                    onFailure = { msg ->
                        binding.btnEnviarComentario.isEnabled = true
                        Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
                    }
                )
            },
            onFailure = {
                binding.btnEnviarComentario.isEnabled = true
            }
        )
    }
}