package br.com.canarinho.redesocial.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.scale
import android.util.Base64
import java.io.ByteArrayOutputStream

class Base64Converter {
    companion object {
        fun drawableToString(drawable: Drawable): String {
            val pictureDrawable = drawable as BitmapDrawable
            val bitmap = pictureDrawable.bitmap.scale(800, 800)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            val imageString = Base64.encodeToString(outputStream.toByteArray(), 0)
            return imageString
        }

        fun stringToBitmap(imageString: String): Bitmap {
            val imageBytes = Base64.decode(imageString, Base64.DEFAULT)
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        }
    }
}