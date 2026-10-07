package com.pame.karsy.core.util

import com.pame.karsy.R
import com.pame.karsy.core.locale.texto

/**
 * Reglas de los formularios de cuenta (registro particular / lote y Editar perfil).
 * Cada función devuelve el mensaje de error o null si el valor es válido; un valor vacío
 * se considera válido aquí (lo obligatorio se revisa aparte). Los máximos son los de la BD.
 */
object FormRules {
    /** Nombre y apellido por separado; juntos forman nombre_mostrar (VARCHAR 100). */
    const val NOMBRE_MAX = 50
    const val LOTE_MAX = 120
    const val DIRECCION_MAX = 120
    const val NUMERO_MAX = 20
    /** cuentas.descripcion_corta (la BD recorta a 300). */
    const val DESCRIPCION_MAX = 300
    /** Lado mínimo del logo del lote en píxeles (más chico se ve borroso). */
    const val LOGO_MIN_PX = 200

    private val CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")
    /** Letras (con acentos y ñ), espacios, guion, apóstrofe y punto ("Ma. José", "O'Neil"). */
    private val NOMBRE = Regex("^[\\p{L}][\\p{L} .'-]*$")
    /** El nombre de un lote puede llevar números y signos comunes ("Autos 4x4 & Cía."). */
    private val NOMBRE_LOTE = Regex("^[\\p{L}\\p{N}][\\p{L}\\p{N} .,'&()-]*$")
    /** "123", "123-A", "123 B", "S/N". */
    private val NUMERO_EXT = Regex("^([0-9]+[A-Za-z]?([ -]?[A-Za-z0-9]+)?|[Ss]/?[Nn])$")

    // ── Filtros al escribir: el campo no acepta caracteres del tipo equivocado ──

    /** Solo dígitos (año, precio, código postal…), cortado a [max] dígitos. */
    fun soloDigitos(texto: String, max: Int = Int.MAX_VALUE) = texto.filter(Char::isDigit).take(max)

    /**
     * Teléfono: solo dígitos, máximo 10. Se permite un "+" al inicio para la lada
     * internacional ("+52" y 10 dígitos); al guardar se quita (ver normalizarTelefono).
     */
    fun telefonoAlEscribir(texto: String): String {
        val digitos = texto.filter(Char::isDigit)
        return if (texto.trimStart().startsWith("+")) "+" + digitos.take(12) else digitos.take(10)
    }

    /** Nombres de persona: letras (con acentos y ñ), espacios, guion, apóstrofe y punto; sin números. */
    fun soloLetras(texto: String) = texto.filter { it.isLetter() || it == ' ' || it == '-' || it == '\'' || it == '.' }

    /** Nombre o apellido al escribir: solo letras y máximo [NOMBRE_MAX]. */
    fun nombreAlEscribir(texto: String) = soloLetras(texto).take(NOMBRE_MAX)

    /** Recorta al escribir: el campo no deja pasar de [max] caracteres. */
    fun maximo(max: Int): (String) -> String = { it.take(max) }

    /** Dominios de correo más usados en México (para sugerir correcciones). */
    private val DOMINIOS = listOf(
        "gmail.com", "hotmail.com", "outlook.com", "yahoo.com", "yahoo.com.mx", "live.com",
        "live.com.mx", "icloud.com", "hotmail.es", "outlook.es", "prodigy.net.mx", "msn.com",
    )

    /**
     * "pame@gmil.com" → "pame@gmail.com": si el dominio está a 1 o 2 letras de uno común,
     * devuelve el correo corregido; si ya es uno común o no se parece a ninguno, null.
     */
    fun sugerenciaCorreo(valor: String): String? {
        val c = limpiarCorreo(valor)
        val usuario = c.substringBefore('@', "")
        val dominio = c.substringAfter('@', "")
        if (usuario.isEmpty() || dominio.length < 3 || dominio in DOMINIOS) return null
        val (mejor, distancia) = DOMINIOS.map { it to distancia(dominio, it) }.minBy { it.second }
        // Dominios cortos con 2 cambios ya serían otro dominio (p. ej. "gmx.com").
        val maximo = if (dominio.length <= 6) 1 else 2
        return if (distancia in 1..maximo) "$usuario@$mejor" else null
    }

    /** Distancia de Levenshtein: cuántas letras hay que cambiar, quitar o agregar. */
    private fun distancia(a: String, b: String): Int {
        var anterior = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val actual = IntArray(b.length + 1).also { it[0] = i }
            for (j in 1..b.length) {
                val costo = if (a[i - 1] == b[j - 1]) 0 else 1
                actual[j] = minOf(actual[j - 1] + 1, anterior[j] + 1, anterior[j - 1] + costo)
            }
            anterior = actual
        }
        return anterior[b.length]
    }

    /** Correo sin espacios y en minúsculas, como se guarda. */
    fun limpiarCorreo(correo: String) = correo.trim().lowercase()

    /** Quita espacios dobles: "juan   pérez " → "juan pérez". */
    fun limpiarEspacios(texto: String) = texto.trim().replace(Regex("\\s+"), " ")

    fun correo(valor: String): String? {
        val c = limpiarCorreo(valor)
        return if (c.isEmpty() || CORREO.matches(c)) null else texto(R.string.form_error_email)
    }

    fun nombrePersona(valor: String): String? {
        val v = limpiarEspacios(valor)
        return when {
            v.isEmpty() -> null
            !NOMBRE.matches(v) -> texto(R.string.form_error_name_letters)
            v.count { it.isLetter() } < 2 -> texto(R.string.form_error_min, 2)
            v.length > NOMBRE_MAX -> texto(R.string.form_error_max, NOMBRE_MAX)
            else -> null
        }
    }

    fun nombreLote(valor: String): String? {
        val v = limpiarEspacios(valor)
        return when {
            v.isEmpty() -> null
            !NOMBRE_LOTE.matches(v) -> texto(R.string.form_error_lot_name)
            v.length < 3 -> texto(R.string.form_error_min, 3)
            v.length > LOTE_MAX -> texto(R.string.form_error_max, LOTE_MAX)
            else -> null
        }
    }

    /** Calle o colonia. */
    fun textoDireccion(valor: String): String? {
        val v = limpiarEspacios(valor)
        return when {
            v.isEmpty() -> null
            v.length < 3 -> texto(R.string.form_error_min, 3)
            v.length > DIRECCION_MAX -> texto(R.string.form_error_max, DIRECCION_MAX)
            else -> null
        }
    }

    fun numeroExterior(valor: String): String? {
        val v = limpiarEspacios(valor)
        return when {
            v.isEmpty() -> null
            v.length > NUMERO_MAX -> texto(R.string.form_error_max, NUMERO_MAX)
            !NUMERO_EXT.matches(v) -> texto(R.string.form_error_street_number)
            else -> null
        }
    }

    fun codigoPostal(valor: String): String? {
        val v = valor.trim()
        return if (v.isEmpty() || (v.length == 5 && v.all { it.isDigit() })) null else texto(R.string.form_error_zip)
    }

    /**
     * Teléfono de México a 10 dígitos: quita espacios, guiones y la lada +52 / 52.
     * "871 123 4567", "+52 871 123 4567" y "528711234567" → "8711234567".
     */
    fun normalizarTelefono(valor: String): String {
        val digitos = valor.filter { it.isDigit() }
        return if (digitos.length == 12 && digitos.startsWith("52")) digitos.drop(2) else digitos
    }

    fun telefono(valor: String): String? {
        if (valor.isBlank()) return null
        val n = normalizarTelefono(valor)
        return when {
            n.length != 10 -> texto(R.string.form_error_phone_10)
            esTelefonoFalso(n) -> texto(R.string.form_error_phone_fake)
            else -> null
        }
    }

    /**
     * Números que nadie tiene: todos iguales (5555555555), escaleras (0123456789,
     * 9876543210), dos dígitos repetidos (1212121212) o casi todos iguales (8888888881).
     */
    private fun esTelefonoFalso(n: String): Boolean {
        val d = n.map { it - '0' }
        val escalera = (1 until d.size).all { d[it] == (d[it - 1] + 1) % 10 } ||
            (1 until d.size).all { d[it] == (d[it - 1] + 9) % 10 }
        val parRepetido = n.chunked(2).toSet().size == 1
        val casiIguales = n.groupingBy { it }.eachCount().values.max() >= 8
        return escalera || parRepetido || casiIguales
    }

    // ── Código postal ↔ estado (esquema de SEPOMEX: los 2 primeros dígitos) ──

    private val CP_ESTADO: List<Pair<IntRange, String>> = listOf(
        1..16 to "Ciudad de México", 20..20 to "Aguascalientes", 21..22 to "Baja California",
        23..23 to "Baja California Sur", 24..24 to "Campeche", 25..27 to "Coahuila", 28..28 to "Colima",
        29..30 to "Chiapas", 31..33 to "Chihuahua", 34..35 to "Durango", 36..38 to "Guanajuato",
        39..41 to "Guerrero", 42..43 to "Hidalgo", 44..49 to "Jalisco", 50..57 to "Estado de México",
        58..61 to "Michoacán", 62..62 to "Morelos", 63..63 to "Nayarit", 64..67 to "Nuevo León",
        68..71 to "Oaxaca", 72..75 to "Puebla", 76..76 to "Querétaro", 77..77 to "Quintana Roo",
        78..79 to "San Luis Potosí", 80..82 to "Sinaloa", 83..85 to "Sonora", 86..86 to "Tabasco",
        87..89 to "Tamaulipas", 90..90 to "Tlaxcala", 91..96 to "Veracruz", 97..97 to "Yucatán",
        98..99 to "Zacatecas",
    )

    /** Estado al que pertenece un código postal de 5 dígitos, o null si no se reconoce. */
    fun estadoDelCodigoPostal(cp: String): String? {
        if (cp.length != 5 || !cp.all(Char::isDigit)) return null
        val prefijo = cp.take(2).toInt()
        return CP_ESTADO.firstOrNull { prefijo in it.first }?.second
    }

    /**
     * Aviso (no bloquea) si el código postal es de otro estado:
     * "Este código postal es de Jalisco, no de Coahuila."
     */
    fun avisoCodigoPostalEstado(cp: String, estado: String): String? {
        if (estado.isBlank()) return null
        val delCp = estadoDelCodigoPostal(cp) ?: return null
        return if (sinAcentos(delCp) == sinAcentos(estado) || sinAcentos(estado).startsWith(sinAcentos(delCp))) null
        else texto(R.string.form_warning_zip_state, delCp, estado)
    }

    private fun sinAcentos(t: String) =
        java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").lowercase()

    /** "juan  carlos de la o" → "Juan Carlos de la O" (artículos y preposiciones en minúscula). */
    fun capitalizarNombre(texto: String): String {
        val menores = setOf("de", "del", "la", "las", "los", "y", "e", "van", "von")
        return limpiarEspacios(texto).split(" ").mapIndexed { i, palabra ->
            val baja = palabra.lowercase()
            if (i > 0 && baja in menores) baja
            // Respeta guiones y apóstrofes: "peña-nieto" → "Peña-Nieto", "o'neil" → "O'Neil".
            else baja.split("-").joinToString("-") { parte ->
                parte.split("'").joinToString("'") { it.replaceFirstChar(Char::titlecase) }
            }
        }.joinToString(" ")
    }
}
