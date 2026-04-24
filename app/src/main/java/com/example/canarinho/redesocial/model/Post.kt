package br.com.canarinho.redesocial.model

import android.graphics.Bitmap
import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val descricao: String = "",
    val imagem: Bitmap? = null,
    val emailAutor: String = "",
    val usernameAutor: String = "",
    val fotoAutor: Bitmap? = null,
    val data: Timestamp = Timestamp.now(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val cidade: String = ""
)