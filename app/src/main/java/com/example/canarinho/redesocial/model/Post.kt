package br.com.canarinho.redesocial.model

import android.graphics.Bitmap
import com.google.firebase.Timestamp

data class Post(
    val id: String = "",
    val descricao: String = "",
    val imagem: Bitmap? = null,
    val emailAutor: String = "",
    val data: Timestamp = Timestamp.now()
)