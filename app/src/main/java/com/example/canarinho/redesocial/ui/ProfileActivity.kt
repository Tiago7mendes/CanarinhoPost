package br.com.canarinho.redesocial.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
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
                        } catch (e: Exception) { }
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

        binding.btnAlterarSenha.setOnClickListener {
            val visivel = binding.cardAlterarSenha.visibility == View.VISIBLE
            binding.cardAlterarSenha.visibility = if (visivel) View.GONE else View.VISIBLE
        }

        binding.btnConfirmarSenha.setOnClickListener {
            alterarSenha()
        }

        // Sair da conta
        binding.btnSair.setOnClickListener {
            userAuth.logout()
            val intent = Intent(this, LoginActivity::class.java).apply {
                // Limpa a pilha de activities — não volta para home com o botão voltar
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
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
                Toast.makeText(this, "Perfil salvo! 💚", Toast.LENGTH_SHORT).show()
                finish()
            },
            onFailure = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun alterarSenha() {
        val senhaAtual = binding.edtSenhaAtual.text.toString().trim()
        val novaSenha = binding.edtNovaSenha.text.toString().trim()
        val confirmarSenha = binding.edtConfirmarNovaSenha.text.toString().trim()

        if (senhaAtual.isEmpty() || novaSenha.isEmpty() || confirmarSenha.isEmpty()) {
            Toast.makeText(this, "Preencha todos os campos de senha", Toast.LENGTH_SHORT).show()
            return
        }
        if (novaSenha != confirmarSenha) {
            Toast.makeText(this, "As novas senhas não coincidem", Toast.LENGTH_SHORT).show()
            return
        }
        if (novaSenha.length < 6) {
            Toast.makeText(this, "A senha deve ter pelo menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnConfirmarSenha.isEnabled = false

        userAuth.alterarSenha(senhaAtual, novaSenha,
            onSuccess = {
                Toast.makeText(this, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()
                binding.edtSenhaAtual.setText("")
                binding.edtNovaSenha.setText("")
                binding.edtConfirmarNovaSenha.setText("")
                binding.cardAlterarSenha.visibility = View.GONE
                binding.btnConfirmarSenha.isEnabled = true
            },
            onFailure = { msg ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                binding.btnConfirmarSenha.isEnabled = true
            }
        )
    }
}