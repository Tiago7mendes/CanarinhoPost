package br.com.canarinho.redesocial.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.canarinho.redesocial.adapter.PostAdapter
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.PostDAO
import br.com.canarinho.redesocial.dao.UserDAO
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var userDAO: UserDAO
    private lateinit var postDAO: PostDAO
    private val userAuth = UserAuth()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDAO = UserDAO(this)
        postDAO = PostDAO(this)

        carregarDadosUsuario()
        setupListeners()
        carregarFeed()
    }

    private fun carregarDadosUsuario() {
        val email = userAuth.getEmailUsuarioLogado() ?: return
        userDAO.getByEmail(email,
            onSuccess = { user ->
                user?.let {
                    try {
                        val bitmap = Base64Converter.stringToBitmap(it.fotoPerfil)
                        binding.imgFotoPerfil.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Mantém imagem padrão
                    }
                    binding.txtUsername.text = "@${it.username}"
                    binding.txtNomeCompleto.text = it.nomeCompleto
                } ?: Toast.makeText(this, "Perfil não encontrado", Toast.LENGTH_SHORT).show()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        binding.btnEditarPerfil.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        binding.btnCarregarFeed.setOnClickListener {
            carregarFeed()
        }
    }

    private fun carregarFeed() {
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE

        postDAO.getAll(
            onSuccess = { posts ->
                binding.progressBar.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                val adapter = PostAdapter(posts.toTypedArray())
                binding.recyclerView.layoutManager = LinearLayoutManager(this)
                binding.recyclerView.adapter = adapter
            },
            onFailure = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao carregar feed: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }
}