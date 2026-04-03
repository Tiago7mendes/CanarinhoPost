package br.com.canarinho.redesocial.model

import android.graphics.Bitmap

data class Post(
    val descricao: String = "",
    val imagem: Bitmap? = null
)