package br.com.canarinho.redesocial.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.PostDAO
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityCreatePostBinding

class CreatePostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var postDAO: PostDAO
    private val userAuth = UserAuth()

    // Quando não nulo, estamos editando um post existente
    private var postIdEdicao: String? = null
    private var imagemSelecionada = false

    private val galeria = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            binding.imgPreview.setImageURI(it)
            binding.imgPreview.visibility = View.VISIBLE
            imagemSelecionada = true
        } ?: Toast.makeText(this, "Nenhuma foto selecionada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatePostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        postDAO = PostDAO(this)

        // Verifica se veio com dados para edição
        postIdEdicao = intent.getStringExtra("POST_ID")
        val descricaoEdicao = intent.getStringExtra("POST_DESCRICAO")

        if (postIdEdicao != null) {
            binding.btnPublicar.text = "SALVAR EDIÇÃO"
            binding.btnSelecionarFoto.text = "Trocar foto (opcional)"
            binding.edtDescricao.setText(descricaoEdicao)
            imagemSelecionada = true
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSelecionarFoto.setOnClickListener {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnPublicar.setOnClickListener {
            if (postIdEdicao != null) editarPost() else publicarPost()
        }

        binding.btnVoltar.setOnClickListener { finish() }
    }

    private fun publicarPost() {
        val descricao = binding.edtDescricao.text.toString().trim()
        val email = userAuth.getEmailUsuarioLogado() ?: return

        if (descricao.isEmpty()) {
            Toast.makeText(this, "Escreva uma descrição", Toast.LENGTH_SHORT).show()
            return
        }
        if (!imagemSelecionada) {
            Toast.makeText(this, "Selecione uma foto", Toast.LENGTH_SHORT).show()
            return
        }

        val imageString = Base64Converter.drawableToString(binding.imgPreview.drawable)
        setCarregando(true)

        postDAO.save(descricao, imageString, email,
            onSuccess = {
                Toast.makeText(this, "Post publicado! 🇧🇷", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { msg ->
                setCarregando(false)
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun editarPost() {
        val descricao = binding.edtDescricao.text.toString().trim()
        val id = postIdEdicao ?: return

        if (descricao.isEmpty()) {
            Toast.makeText(this, "Escreva uma descrição", Toast.LENGTH_SHORT).show()
            return
        }

        // Se o usuário trocou a foto usa a nova, senão mantém a original
        val imageString = if (binding.imgPreview.visibility == View.VISIBLE) {
            Base64Converter.drawableToString(binding.imgPreview.drawable)
        } else {
            intent.getStringExtra("POST_IMAGE_STRING") ?: ""
        }

        setCarregando(true)

        postDAO.update(id, descricao, imageString,
            onSuccess = {
                Toast.makeText(this, "Post atualizado!", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { msg ->
                setCarregando(false)
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setCarregando(carregando: Boolean) {
        binding.btnPublicar.isEnabled = !carregando
        binding.progressBar.visibility = if (carregando) View.VISIBLE else View.GONE
    }
}