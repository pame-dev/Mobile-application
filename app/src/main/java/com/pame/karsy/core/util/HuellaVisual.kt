package com.pame.karsy.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

/**
 * Huella visual de una foto (dHash de 64 bits): sirve para reconocer la misma foto aunque se
 * haya vuelto a guardar, cambiado de tamaño o tapado las placas. Igual que
 * web/src/lib/huellaVisual.ts (mismo cálculo, para comparar fotos de web y app).
 *
 * Cómo se calcula: la foto se reduce a 576×512, se promedia en bloques de 8×8 a [ANCHO]×[ALTO]
 * en grises, otra vez en bloques de 8×8 (queda de 9×8) y cada bit dice si un cuadro es más
 * claro que el de su derecha. Los promedios por bloques hacen que web y app den la misma huella
 * aunque cada una reduzca la imagen con su propio filtro.
 */
object HuellaVisual {
    const val ANCHO = 72
    const val ALTO = 64
    /** Bits distintos con los que dos huellas se consideran la misma foto (de 64). */
    const val MAX_DISTANCIA = 4

    /** Huella (16 caracteres hex) a partir de ANCHO×ALTO valores de gris (0–255), fila por fila. */
    fun deGrises(grises: DoubleArray): String {
        val celdas = Array(8) { y ->
            DoubleArray(9) { x ->
                var suma = 0.0
                for (j in 0 until 8) for (i in 0 until 8) suma += grises[(y * 8 + j) * ANCHO + x * 8 + i]
                suma / 64
            }
        }
        val hex = StringBuilder()
        var nibble = 0
        var bits = 0
        for (y in 0 until 8) for (x in 0 until 8) {
            nibble = (nibble shl 1) or (if (celdas[y][x] > celdas[y][x + 1]) 1 else 0)
            if (++bits % 4 == 0) {
                hex.append(Integer.toHexString(nibble))
                nibble = 0
            }
        }
        return hex.toString()
    }

    /** Cuántos bits distintos hay entre dos huellas (0 = idénticas). */
    fun distancia(a: String, b: String): Int =
        (0 until 16).sumOf { Integer.bitCount(Character.digit(a[it], 16) xor Character.digit(b[it], 16)) }

    fun mismaFoto(a: String, b: String): Boolean = distancia(a, b) <= MAX_DISTANCIA

    /** Pocas a la vez: cada una decodifica la foto completa (memoria). */
    private val turnos = Semaphore(2)

    /** Huella visual de una foto (respeta la rotación del celular); null si no se pudo leer. */
    suspend fun calcular(context: Context, uri: Uri): String? = turnos.withPermit {
        withContext(Dispatchers.IO) {
            runCatching {
                var bitmap: Bitmap = TapaPlacas.leer(context, uri) ?: return@runCatching null
                // A la mitad mientras sobre (con suavizado) y luego a 576×512.
                while (bitmap.width / 2 >= ANCHO * 8 && bitmap.height / 2 >= ALTO * 8) {
                    val mitad = bitmap.scale(bitmap.width / 2, bitmap.height / 2)
                    bitmap.recycle()
                    bitmap = mitad
                }
                val medio = bitmap.scale(ANCHO * 8, ALTO * 8)
                if (medio !== bitmap) bitmap.recycle()
                val pixeles = IntArray(ANCHO * 8 * ALTO * 8)
                medio.getPixels(pixeles, 0, ANCHO * 8, 0, 0, ANCHO * 8, ALTO * 8)
                medio.recycle()
                deGrises(grisesPorBloques(pixeles))
            }.getOrNull()
        }
    }

    /** ARGB de (ANCHO·8)×(ALTO·8) → ANCHO×ALTO grises, promediando bloques de 8×8 (igual que la web). */
    fun grisesPorBloques(argb: IntArray): DoubleArray {
        val ancho = ANCHO * 8
        val grises = DoubleArray(ANCHO * ALTO)
        for (y in 0 until ALTO * 8) for (x in 0 until ancho) {
            val c = argb[y * ancho + x]
            grises[(y / 8) * ANCHO + x / 8] += (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) / 64
        }
        return grises
    }
}
