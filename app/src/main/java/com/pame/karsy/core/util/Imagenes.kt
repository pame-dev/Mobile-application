package com.pame.karsy.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** Lee una imagen elegida en la galería y la reduce a JPEG para subirla a Storage. */
object Imagenes {

    suspend fun jpegBytes(context: Context, uri: Uri, maxLado: Int = 1600, calidad: Int = 82): ByteArray =
        withContext(Dispatchers.IO) {
            val resolver = context.contentResolver
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }

            var muestra = 1
            while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= maxLado) muestra *= 2

            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
            } ?: throw UserFacingException("No se pudo leer la imagen.")

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
