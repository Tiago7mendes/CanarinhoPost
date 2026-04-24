package br.com.canarinho.redesocial.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import br.com.canarinho.redesocial.auth.UserAuth
import br.com.canarinho.redesocial.dao.PostDAO
import br.com.canarinho.redesocial.util.Base64Converter
import br.com.canarinho.redesocial.util.LocalizacaoHelper
import com.example.canarinho.redesocial.databinding.ActivityCreatePostBinding

class CreatePostActivity : AppCompatActivity(), LocalizacaoHelper.Callback {

    private lateinit var binding: ActivityCreatePostBinding
    private lateinit var postDAO: PostDAO
    private val userAuth = UserAuth()

    private var postIdEdicao: String? = null
    private var imagemSelecionada = false

    // Localização obtida antes de publicar
    private var latitudeAtual: Double = 0.0
    private var longitudeAtual: Double = 0.0
    private var cidadeAtual: String = ""

    private val LOCATION_PERMISSION_CODE = 1001

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
        postIdEdicao = intent.getStringExtra("POST_ID")
        val descricaoEdicao = intent.getStringExtra("POST_DESCRICAO")

        if (postIdEdicao != null) {
            binding.txtTituloTela.text = "Editar Post"
            binding.btnPublicar.text = "SALVAR EDIÇÃO"
            binding.edtDescricao.setText(descricaoEdicao)
            binding.btnSelecionarFoto.text = "📷  Trocar foto (opcional)"
            imagemSelecionada = true
        }

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSelecionarFoto.setOnClickListener {
            galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnPublicar.setOnClickListener {
            if (postIdEdicao != null) {
                editarPost()
            } else {
                solicitarLocalizacaoEPublicar()
            }
        }
        binding.btnVoltar.setOnClickListener { finish() }
    }

    // Ao publicar, primeiro obtém a localização depois salva
    private fun solicitarLocalizacaoEPublicar() {
        val descricao = binding.edtDescricao.text.toString().trim()
        if (descricao.isEmpty()) {
            Toast.makeText(this, "Escreva uma descrição", Toast.LENGTH_SHORT).show()
            return
        }
        if (!imagemSelecionada) {
            Toast.makeText(this, "Selecione uma foto", Toast.LENGTH_SHORT).show()
            return
        }

        setCarregando(true)
        binding.txtStatusLocalizacao.visibility = View.VISIBLE
        binding.txtStatusLocalizacao.text = "📍 Obtendo localização..."

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_CODE
            )
        } else {
            val helper = LocalizacaoHelper(applicationContext)
            helper.obterLocalizacaoAtual(this)
        }
    }

    // Callback do LocalizacaoHelper — chamado quando a localização chega
    override fun onLocalizacaoRecebida(endereco: Address, latitude: Double, longitude: Double) {
        latitudeAtual = latitude
        longitudeAtual = longitude
        // Tenta cidade, senão usa subAdminArea, senão país
        cidadeAtual = endereco.locality
            ?: endereco.subAdminArea
                    ?: endereco.adminArea
                    ?: endereco.countryName
                    ?: ""

        runOnUiThread {
            binding.txtStatusLocalizacao.text = "📍 $cidadeAtual"
            publicarPost()
        }
    }

    override fun onErro(mensagem: String) {
        runOnUiThread {
            // Se falhar a localização, publica mesmo assim sem cidade
            binding.txtStatusLocalizacao.text = "📍 Localização indisponível"
            Toast.makeText(this, "Localização não obtida, publicando sem ela", Toast.LENGTH_SHORT).show()
            publicarPost()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            val helper = LocalizacaoHelper(applicationContext)
            helper.obterLocalizacaoAtual(this)
        } else {
            Toast.makeText(this, "Localização não concedida, publicando sem ela", Toast.LENGTH_SHORT).show()
            publicarPost()
        }
    }

    private fun publicarPost() {
        val descricao = binding.edtDescricao.text.toString().trim()
        val email = userAuth.getEmailUsuarioLogado() ?: return
        val imageString = Base64Converter.drawableToString(binding.imgPreview.drawable)

        postDAO.save(descricao, imageString, email, latitudeAtual, longitudeAtual, cidadeAtual,
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