package br.com.canarinho.redesocial.model

import android.graphics.Bitmap
import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val texto: String = "",
    val emailAutor: String = "",
    val usernameAutor: String = "",
    val fotoAutor: Bitmap? = null,
    val data: Timestamp = Timestamp.now()
)