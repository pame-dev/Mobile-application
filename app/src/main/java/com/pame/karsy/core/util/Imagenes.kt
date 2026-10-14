package com.pame.karsy.core.util

import android.content.Context
import java.net.URL
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Lee una imagen (de la galería, un archivo o la URL pública de una foto ya publicada) y la
 * reduce a JPEG para subirla a Storage.
 */
object Imagenes {

    suspend fun jpegBytes(context: Context, uri: Uri, maxLado: Int = 1600, calidad: Int = 82): ByteArray =
        withContext(Dispatchers.IO) {
            val bytes = runCatching {
                if (uri.scheme == "http" || uri.scheme == "https") URL(uri.toString()).openStream().use { it.readBytes() }
                else context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull() ?: throw UserFacingException(texto(R.string.core_error_read_image))
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)

            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) muestra *= 2

            val bitmap = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestra }
            ) ?: throw UserFacingException(texto(R.string.core_error_read_image))

            val escala = maxLado.toFloat() / maxOf(bitmap.width, bitmap.height)
            val final = if (escala < 1f) {
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * escala).toInt(), (bitmap.height * escala).toInt(), true)
            } else bitmap

            ByteArrayOutputStream().use { out ->
                final.compress(Bitmap.CompressFormat.JPEG, calidad, out)
                out.toByteArray()
            }
        }
}
