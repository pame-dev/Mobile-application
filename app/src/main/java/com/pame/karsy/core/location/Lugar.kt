package com.pame.karsy.core.location

import java.text.Normalizer
import java.util.Locale

/** Municipio y estado (de un perfil o elegidos a mano) para "Cerca de mí". */
data class Lugar(val municipio: String, val estado: String)

/**
 * Las publicaciones y los perfiles guardan estado y municipio como texto (no coordenadas),
 * así que los lugares se comparan por nombre.
 */
object Lugares {

    /** Compara nombres sin mayúsculas ni acentos: "Coahuila" = "Coahuila de Zaragoza". */
    fun mismoLugar(a: String, b: String): Boolean {
        val x = normalizar(a)
        val y = normalizar(b)
        if (x.isEmpty() || y.isEmpty()) return false
        return x == y || x.startsWith(y) || y.startsWith(x)
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace("estado de ", "")
            .trim()
}
