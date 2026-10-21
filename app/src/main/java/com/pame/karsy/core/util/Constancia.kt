package com.pame.karsy.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto

/** Archivo de la Constancia de Situación Fiscal elegido en el teléfono (aún sin subir). */
data class ArchivoConstancia(
    val uri: Uri,
    val nombre: String,
    /** Tamaño en bytes. */
    val tamano: Long,
    /** Tipo ya normalizado: application/pdf, image/jpeg o image/png. */
    val mime: String,
)

/**
 * Constancia de Situación Fiscal (CSF) del SAT con la que un lote pide la insignia
 * "Lote verificado". Mismas reglas que el bucket privado constancias-fiscales:
 * PDF, JPG o PNG de máximo 5 MB.
 */
object Constancia {
    /** verificaciones_usuario.tipo_documento de la constancia. */
    const val TIPO_DOCUMENTO = "constancia_situacion_fiscal"

    /** Valores de verificaciones_usuario.estado; SIN_DOCUMENTO = el usuario aún no envía ninguno. */
    const val SIN_DOCUMENTO = "sin_documento"
    const val PENDIENTE = "pendiente"
    const val VERIFICADO = "verificado"
    const val RECHAZADO = "rechazado"

    /**
     * Estado de verificación que muestra el perfil: el del último documento enviado; si nunca envió
     * ninguno, 'verificado' si la cuenta ya tiene la insignia o [SIN_DOCUMENTO].
     */
    fun estadoPerfil(verificado: Boolean, ultimoEstado: String?): String =
        ultimoEstado ?: if (verificado) VERIFICADO else SIN_DOCUMENTO

    const val MAX_MB = 5
    const val MAX_BYTES = MAX_MB * 1024L * 1024L

    /** Lo que ofrece el selector de archivos; el tipo real se revisa después con [mimeValido]. */
    val TIPOS_SELECTOR = arrayOf("application/pdf", "image/*")

    /** Tipo aceptado y normalizado (image/jpg → image/jpeg), o null si no se acepta (webp, heic…). */
    fun mimeValido(mime: String?): String? = when (mime?.trim()?.lowercase()) {
        "application/pdf" -> "application/pdf"
        "image/jpeg", "image/jpg", "image/pjpeg" -> "image/jpeg"
        "image/png" -> "image/png"
        else -> null
    }

    /** Extensión del archivo en el bucket según su tipo. */
    fun extension(mime: String): String = when (mimeValido(mime)) {
        "application/pdf" -> "pdf"
        "image/png" -> "png"
        else -> "jpg"
    }

    /** Id del texto de error para ese tipo y tamaño, o null si el archivo sirve. */
    fun errorDe(mime: String?, tamano: Long?): Int? = when {
        mimeValido(mime) == null -> R.string.form_error_constancia_format
        tamano == null || tamano <= 0 -> R.string.form_error_constancia_unreadable
        tamano > MAX_BYTES -> R.string.form_error_constancia_size
        else -> null
    }

    /**
     * Revisa el archivo elegido (tipo según el ContentResolver y tamaño con OpenableColumns).
     * Lanza [UserFacingException] con el motivo si no sirve.
     */
    fun revisar(context: Context, uri: Uri): ArchivoConstancia {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri)
        var nombre: String? = null
        var tamano: Long? = null
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val iNombre = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val iTamano = c.getColumnIndex(OpenableColumns.SIZE)
                    if (iNombre >= 0 && !c.isNull(iNombre)) nombre = c.getString(iNombre)
                    if (iTamano >= 0 && !c.isNull(iTamano)) tamano = c.getLong(iTamano)
                }
            }
        }
        errorDe(mime, tamano)?.let { throw UserFacingException(error(it)) }
        val tipo = mimeValido(mime)!!
        return ArchivoConstancia(
            uri = uri,
            nombre = nombre?.takeIf { it.isNotBlank() } ?: "constancia.${extension(tipo)}",
            tamano = tamano!!,
            mime = tipo,
        )
    }

    /**
     * Lee el archivo (en segundo plano) y vuelve a revisar el tamaño por si el proveedor informó mal.
     * Lee como máximo MAX_BYTES + 1, así un archivo enorme no se carga completo en memoria.
     */
    suspend fun bytes(context: Context, archivo: ArchivoConstancia): ByteArray = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(archivo.uri)?.use { entrada ->
            val salida = ByteArrayOutputStream()
            val bufer = ByteArray(8 * 1024)
            while (salida.size() <= MAX_BYTES) {
                val n = entrada.read(bufer)
                if (n < 0) break
                salida.write(bufer, 0, n)
            }
            salida.toByteArray()
        } ?: throw UserFacingException(texto(R.string.form_error_constancia_unreadable))
        errorDe(archivo.mime, bytes.size.toLong())?.let { throw UserFacingException(error(it)) }
        bytes
    }

    private fun error(id: Int): String =
        if (id == R.string.form_error_constancia_size) texto(id, MAX_MB) else texto(id)
}
