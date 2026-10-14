package com.pame.karsy.core.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * ¿La foto parece de un auto? Lo revisa ML Kit en el teléfono (modelo incluido en la
 * app, sin internet ni costo). Se usa en las 4 fotos de ángulos al publicar; el tablero
 * y las fotos adicionales no se revisan. La web hace lo mismo con TensorFlow.js
 * (web/src/lib/detectorAutos.ts).
 */
object DetectorAutos {
    /** Fotos obligatorias que deben mostrar el auto: frontal, trasera y laterales. */
    const val ANGULOS_CON_AUTO = 4

    /**
     * Etiquetas de ML Kit que cuentan como "se ve un auto". "Vehicle", "Wheel" y "Tire" no
     * sirven solas: también salen con barcos, bicicletas o texturas.
     */
    private val ETIQUETAS_AUTO = setOf("Car", "Van", "Bumper")

    /** Confianza mínima de alguna de esas etiquetas. */
    const val CONFIANZA_MIN = 0.5f

    private val labeler by lazy {
        // Umbral bajo para recibir también las etiquetas débiles; la decisión es esAuto().
        ImageLabeling.getClient(ImageLabelerOptions.Builder().setConfidenceThreshold(0.1f).build())
    }

    /** true / false; null si no se pudo leer o analizar la foto (en ese caso no se bloquea). */
    suspend fun pareceAuto(context: Context, uri: Uri): Boolean? = withContext(Dispatchers.IO) {
        runCatching { esAuto(etiquetas(InputImage.fromFilePath(context, uri))) }.getOrNull()
    }

    suspend fun etiquetas(image: InputImage): List<ImageLabel> = suspendCancellableCoroutine { cont ->
        labeler.process(image)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resume(emptyList()) }
    }

    fun esAuto(etiquetas: List<ImageLabel>): Boolean =
        etiquetas.any { it.text in ETIQUETAS_AUTO && it.confidence >= CONFIANZA_MIN }
}
