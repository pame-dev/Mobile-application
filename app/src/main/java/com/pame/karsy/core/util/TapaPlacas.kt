package com.pame.karsy.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URL
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Pixela las placas que aparezcan en una foto antes de subirla (privacidad del vendedor).
 * ML Kit encuentra el texto de la foto en el teléfono (sin internet) y se tapa el que tiene
 * forma de placa: 5 a 8 letras y números con al menos una letra y dos números
 * ("DQJ-0982", "SIY-150-B"), así no se tapan "MAZDA3", "Gol" ni el kilometraje.
 * La web hace lo mismo con PaddleOCR (web/src/lib/tapaPlacas.ts).
 */
object TapaPlacas {

    private val PLACA = Regex("^(?=.*[A-Z])(?=(?:.*\\d){2})[A-Z0-9]{5,8}$")

    /** Mismo tamaño máximo con el que se suben las fotos (Imagenes.jpegBytes). */
    private const val MAX_LADO = 1600

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    fun parecePlaca(texto: String): Boolean =
        PLACA.matches(texto.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' })

    /**
     * Copia de la foto (ya derecha según su EXIF y reducida) con las placas pixeladas.
     * Con [soloSiHayPlaca] (fotos que ya están en Storage, al editar) devuelve la misma [uri]
     * si no encontró placa, para no volver a subirla. Acepta fotos de la galería o URLs
     * públicas. Si no se puede leer o analizar, devuelve [uri] para no bloquear al usuario.
     */
    suspend fun tapar(context: Context, uri: Uri, soloSiHayPlaca: Boolean = false): Uri = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = leer(context, uri) ?: return@runCatching uri
            val zonas = reconocer(bitmap)?.let(::zonasDePlaca).orEmpty()
            if (zonas.isEmpty() && soloSiHayPlaca) return@runCatching uri
            zonas.forEach { pixelar(bitmap, it) }
            val dir = File(context.cacheDir, "fotos_publicar").apply { mkdirs() }
            val archivo = File(dir, "${UUID.randomUUID()}.jpg")
            archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            Uri.fromFile(archivo)
        }.getOrDefault(uri)
    }

    /** Recuadros de las líneas (o grupos de 1–3 palabras) que parecen placa. */
    private fun zonasDePlaca(texto: Text): List<Rect> = texto.textBlocks.flatMap { it.lines }.flatMap { linea ->
        val caja = linea.boundingBox
        if (parecePlaca(linea.text) && caja != null) return@flatMap listOf(caja)
        // La placa puede venir junto a otro texto en la misma línea ("MEX DQJ-0982").
        val palabras = linea.elements
        buildList {
            for (i in palabras.indices) for (n in 1..3) {
                val grupo = palabras.subList(i, minOf(i + n, palabras.size))
                if (grupo.size == n && parecePlaca(grupo.joinToString("") { it.text })) {
                    val cajas = grupo.mapNotNull { it.boundingBox }
                    if (cajas.isNotEmpty()) add(Rect(cajas[0]).apply { cajas.forEach { union(it) } })
                }
            }
        }
    }

    /** Pixelado grueso (unos 6 cuadros a lo ancho): ya no se puede leer. Agranda la zona un poco. */
    private fun pixelar(bitmap: Bitmap, zona: Rect) {
        val mx = (zona.width() * 0.2f).toInt() + 4
        val my = (zona.height() * 0.35f).toInt() + 4
        val r = Rect(zona.left - mx, zona.top - my, zona.right + mx, zona.bottom + my)
        if (!r.intersect(0, 0, bitmap.width, bitmap.height) || r.width() < 2 || r.height() < 2) return
        val recorte = Bitmap.createBitmap(bitmap, r.left, r.top, r.width(), r.height())
        val cuadros = 6
        val chico = Bitmap.createScaledBitmap(
            recorte, cuadros, maxOf(1, cuadros * r.height() / r.width()), true
        )
        Canvas(bitmap).drawBitmap(chico, null, r, Paint().apply { isFilterBitmap = false })
    }

    private suspend fun reconocer(bitmap: Bitmap): Text? = suspendCancellableCoroutine { cont ->
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(null) }
    }

    /** Bitmap editable, girado según el EXIF y de máximo MAX_LADO por lado. */
    private fun leer(context: Context, uri: Uri): Bitmap? {
        // Las fotos que ya están publicadas se descargan de su URL pública.
        val bytes = if (uri.scheme == "http" || uri.scheme == "https") {
            URL(uri.toString()).openStream().use { it.readBytes() }
        } else {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } ?: return null
        val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, limites)
        var muestra = 1
        while (maxOf(limites.outWidth, limites.outHeight) / (muestra * 2) >= MAX_LADO) muestra *= 2
        val original = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = muestra }
        ) ?: return null

        val grados = when (ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val escala = minOf(1f, MAX_LADO.toFloat() / maxOf(original.width, original.height))
        val matriz = Matrix().apply { postScale(escala, escala); postRotate(grados) }
        val final = if (escala < 1f || grados != 0f) {
            Bitmap.createBitmap(original, 0, 0, original.width, original.height, matriz, true)
        } else original
        return if (final.isMutable) final else final.copy(Bitmap.Config.ARGB_8888, true)
    }
}
