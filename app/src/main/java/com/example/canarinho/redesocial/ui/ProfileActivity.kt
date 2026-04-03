package br.com.canarinho.redesocial.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.UserDAO
import br.com.canarinho.redesocial.model.User
import br.com.canarinho.redesocial.util.Base64Converter
import com.example.canarinho.redesocial.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private lateinit var userDAO: UserDAO
    private val userAuth = UserAuth()

    private val galeria = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            binding.imgFotoPerfil.setImageURI(it)
        } ?: Toast.makeText(this, "Nenhuma foto selecionada", Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userDAO = UserDAO(this)
        carregarDadosUsuario()
        setupClickListeners()
    }

    private fun carregarDadosUsuario() {
        val email = userAuth.getEmailUsuarioLogado() ?: return
        userDAO.getByEmail(email,
            onSuccess = { user ->
                user?.let {
                    binding.edtUsername.setText(it.username)
                    binding.edtNomeCompleto.setText(it.nomeCompleto)
                    if (it.fotoPerfil.isNotEmpty()) {
                        try {
                            val bitmap = Base64Converter.stringToBitmap(it.fotoPerfil)
                            binding.imgFotoPerfil.setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            // Mantém imagem padrão
                        }
                    }
                }
            },
            onFailure = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupClickListeners() {
        binding.btnAlterarFoto.setOnClickListener {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnSalvar.setOnClickListener {
            salvarPerfil()
        }
    }

    private fun salvarPerfil() {
        val email = userAuth.getEmailUsuarioLogado() ?: return
        val username = binding.edtUsername.text.toString().trim()
        val nomeCompleto = binding.edtNomeCompleto.text.toString().trim()

        if (username.isEmpty() || nomeCompleto.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
            return
        }

        val fotoPerfilString = Base64Converter.drawableToString(binding.imgFotoPerfil.drawable)
        val user = User(email, username, nomeCompleto, fotoPerfilString)

        userDAO.save(user,
            onSuccess = {
                Toast.makeText(this, "Perfil salvo! Bora Brasil! 💚", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            },
            onFailure = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }
}