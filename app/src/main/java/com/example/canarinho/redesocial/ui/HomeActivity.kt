package br.com.canarinho.redesocial.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.canarinho.redesocial.adapter.PostAdapter
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.PostDAO
import br.com.canarinho.redesocial.dao.UserDAO
import br.com.canarinho.redesocial.model.Post
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityHomeBinding

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var userDAO: UserDAO
    private lateinit var postDAO: PostDAO
    private lateinit var adapter: PostAdapter
    private val userAuth = UserAuth()
    private val posts = mutableListOf<Post>()

    // Guarda a cidade buscada — null significa sem filtro
    private var cidadeFiltro: String? = null

    private val criarPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        carregarPrimeiraPagina()
    }

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
            onEdit = { post, _ -> abrirEdicao(post) },
            onDelete = { post, position -> deletarPost(post.id, position) },
            onComment = { post -> abrirComentarios(post) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = recyclerView.layoutManager as LinearLayoutManager
                val ultimoVisivel = lm.findLastVisibleItemPosition()
                val total = adapter.itemCount
                if (dy > 0
                    && total > 0
                    && ultimoVisivel >= total - 2
                    && postDAO.temMaisPosts
                    && !postDAO.carregando
                ) {
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
                }
            },
            onFailure = { msg ->
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupListeners() {
        // Foto de perfil abre ProfileActivity
        binding.cardFotoPerfil.setOnClickListener {
            criarPostLauncher.launch(Intent(this, ProfileActivity::class.java))
        }

        // Logo canarinho atualiza o feed
        binding.btnCarregarFeed.setOnClickListener {
            limparBusca()
            carregarPrimeiraPagina()
        }

        // FAB novo post
        binding.btnNovoPost.setOnClickListener {
            criarPostLauncher.launch(Intent(this, CreatePostActivity::class.java))
        }

        // Buscar por cidade
        binding.btnBuscar.setOnClickListener {
            executarBusca()
        }

        // Buscar ao pressionar "Search" no teclado
        binding.edtBuscarCidade.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executarBusca()
                true
            } else false
        }

        // Limpar busca
        binding.btnLimparBusca.setOnClickListener {
            limparBusca()
            carregarPrimeiraPagina()
        }
    }

    private fun executarBusca() {
        val cidade = binding.edtBuscarCidade.text.toString().trim()
        if (cidade.isEmpty()) {
            Toast.makeText(this, "Digite uma cidade para buscar", Toast.LENGTH_SHORT).show()
            return
        }
        cidadeFiltro = cidade
        binding.btnLimparBusca.visibility = View.VISIBLE
        carregarPrimeiraPagina()
    }

    private fun limparBusca() {
        cidadeFiltro = null
        binding.edtBuscarCidade.setText("")
        binding.btnLimparBusca.visibility = View.GONE
    }

    private fun carregarPrimeiraPagina() {
        postDAO.resetarPaginacao()
        posts.clear()
        adapter.notifyDataSetChanged()
        binding.progressBar.visibility = View.VISIBLE
        binding.recyclerView.visibility = View.GONE
        carregarPagina()
    }

    private fun carregarMais() {
        carregarPagina()
    }

    private fun carregarPagina() {
        postDAO.getPaginado(
            cidade = cidadeFiltro,
            onSuccess = { novosPosts ->
                binding.progressBar.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                if (novosPosts.isNotEmpty()) {
                    adapter.adicionarPosts(novosPosts)
                } else if (posts.isEmpty()) {
                    val msg = if (cidadeFiltro != null)
                        "Nenhum post encontrado em \"$cidadeFiltro\""
                    else "Nenhum post encontrado"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { msg ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun abrirEdicao(post: Post) {
        criarPostLauncher.launch(
            Intent(this, CreatePostActivity::class.java).apply {
                putExtra("POST_ID", post.id)
                putExtra("POST_DESCRICAO", post.descricao)
            }
        )
    }

    private fun abrirComentarios(post: Post) {
        startActivity(Intent(this, CommentsActivity::class.java).apply {
            putExtra("POST_ID", post.id)
        })
    }

    private fun deletarPost(postId: String, position: Int) {
        postDAO.delete(postId,
            onSuccess = {
                adapter.removeItem(position)
                Toast.makeText(this, "Post removido", Toast.LENGTH_SHORT).show()
            },
            onFailure = { msg ->
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }
}