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
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSelecionarFoto.setOnClickListener {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        binding.btnPublicar.setOnClickListener {
            publicarPost()
        }

        binding.btnVoltar.setOnClickListener {
            finish()
        }
    }

    private fun publicarPost() {
        val descricao = binding.edtDescricao.text.toString().trim()
        val email = userAuth.getEmailUsuarioLogado() ?: return

        if (descricao.isEmpty()) {
            Toast.makeText(this, "Escreva uma descrição para o post", Toast.LENGTH_SHORT).show()
            return
        }

        if (!imagemSelecionada) {
            Toast.makeText(this, "Selecione uma foto para o post", Toast.LENGTH_SHORT).show()
            return
        }

        val imageString = Base64Converter.drawableToString(binding.imgPreview.drawable)

        binding.btnPublicar.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        postDAO.save(descricao, imageString, email,
            onSuccess = {
                Toast.makeText(this, "Post publicado! 🇧🇷", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { msg ->
                binding.btnPublicar.isEnabled = true
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro: $msg", Toast.LENGTH_SHORT).show()
            }
        )
    }
}