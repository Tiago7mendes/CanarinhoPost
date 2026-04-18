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
import br.com.canarinho.redesocial.model.Post
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityHomeBinding
import androidx.recyclerview.widget.RecyclerView

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var userDAO: UserDAO
    private lateinit var postDAO: PostDAO
    private lateinit var adapter: PostAdapter
    private val userAuth = UserAuth()
    private val posts = mutableListOf<Post>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDAO = UserDAO(this)
        postDAO = PostDAO(this)

        carregarDadosUsuario()
        setupAdapter()
        setupListeners()
        carregarPrimeiraPagina()
    }

    private fun setupAdapter() {
        val emailLogado = userAuth.getEmailUsuarioLogado() ?: ""
        adapter = PostAdapter(
            posts,
            emailLogado,
            onEdit = { post, position -> abrirEdicao(post, position) },
            onDelete = { post, position -> deletarPost(post.id, position) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Scroll infinito — carrega mais ao chegar no fim
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val ultimoVisivel = layoutManager.findLastVisibleItemPosition()
                val total = adapter.itemCount

                if (dy > 0 && ultimoVisivel >= total - 2 && postDAO.temMaisPosts) {
                    carregarMais()
                }
            }
        })
    }

    private fun carregarDadosUsuario() {
        val email = userAuth.getEmailUsuarioLogado() ?: return
        userDAO.getByEmail(email,
            onSuccess = { user ->
                user?.let {
                    try {
                        val bitmap = Base64Converter.stringToBitmap(it.fotoPerfil)
                        binding.imgFotoPerfil.setImageBitmap(bitmap)
                    } catch (e: Exception) { }
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
        binding.btnNovoPost.setOnClickListener {
            startActivity(Intent(this, CreatePostActivity::class.java))
        }
        binding.btnCarregarFeed.setOnClickListener {
            carregarPrimeiraPagina()
        }
    }

    override fun onResume() {
        super.onResume()
        carregarPrimeiraPagina()
    }

    // Reinicia a paginação e carrega do zero
    private fun carregarPrimeiraPagina() {
        postDAO.resetarPaginacao()
        posts.clear()
        adapter.notifyDataSetChanged()
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        carregarPagina()
    }

    // Carrega a próxima página sem limpar a lista
    private fun carregarMais() {
        if (!postDAO.temMaisPosts) return
        binding.progressBar.visibility = View.VISIBLE
        carregarPagina()
    }

    private fun carregarPagina() {
        postDAO.getPaginado(
            onSuccess = { novosPosts ->
                binding.progressBar.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.adicionarPosts(novosPosts)
            },
            onFailure = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun abrirEdicao(post: Post, position: Int) {
        val intent = Intent(this, CreatePostActivity::class.java).apply {
            putExtra("POST_ID", post.id)
            putExtra("POST_DESCRICAO", post.descricao)
            // Passa a imageString original para caso o usuário não troque a foto
            putExtra("POST_IMAGE_STRING", "")
        }
        startActivity(intent)
    }

    private fun deletarPost(postId: String, position: Int) {
        postDAO.delete(postId,
            onSuccess = {
                adapter.removeItem(position)
                Toast.makeText(this, "Post removido", Toast.LENGTH_SHORT).show()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Erro ao remover: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }
}