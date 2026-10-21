package com.pame.karsy.core.util

/**
 * Datos que vienen dentro de una CURP (para que el admin los compare con lo que muestra
 * gob.mx/curp). Igual que web/src/lib/curp.ts.
 *
 * Posiciones (desde 0): 4–9 fecha AAMMDD, 10 sexo (H, M, X), 11–12 estado de nacimiento,
 * 16 siglo (dígito = 1900–1999, letra = 2000 en adelante).
 */
object Curp {
    /** Claves de estado de la CURP (NE = nacido en el extranjero, aparte). */
    val ESTADOS = mapOf(
        "AS" to "Aguascalientes", "BC" to "Baja California", "BS" to "Baja California Sur", "CC" to "Campeche",
        "CL" to "Coahuila", "CM" to "Colima", "CS" to "Chiapas", "CH" to "Chihuahua", "DF" to "Ciudad de México",
        "DG" to "Durango", "GT" to "Guanajuato", "GR" to "Guerrero", "HG" to "Hidalgo", "JC" to "Jalisco",
        "MC" to "Estado de México", "MN" to "Michoacán", "MS" to "Morelos", "NT" to "Nayarit", "NL" to "Nuevo León",
        "OC" to "Oaxaca", "PL" to "Puebla", "QT" to "Querétaro", "QR" to "Quintana Roo", "SP" to "San Luis Potosí",
        "SL" to "Sinaloa", "SR" to "Sonora", "TC" to "Tabasco", "TS" to "Tamaulipas", "TL" to "Tlaxcala",
        "VZ" to "Veracruz", "YN" to "Yucatán", "ZS" to "Zacatecas",
    )

    /** [sexo]: 'H', 'M' o 'X'. [estado]: clave de dos letras; "NE" = nacido en el extranjero. */
    data class Datos(val anio: Int, val mes: Int, val dia: Int, val sexo: Char, val estado: String)

    private val FORMATO = Regex("^[A-Z]{4}\\d{6}[HMX][A-Z]{2}[A-Z]{3}[A-Z0-9]\\d$")

    /** null si no tiene el formato de una CURP. */
    fun leer(curp: String): Datos? {
        val c = curp.trim().uppercase()
        if (!FORMATO.matches(c)) return null
        val aa = c.substring(4, 6).toInt()
        val mes = c.substring(6, 8).toInt()
        val dia = c.substring(8, 10).toInt()
        val estado = c.substring(11, 13)
        val anio = (if (c[16].isDigit()) 1900 else 2000) + aa
        if (mes !in 1..12 || dia !in 1..diasDelMes(anio, mes) || (estado != "NE" && estado !in ESTADOS)) return null
        return Datos(anio, mes, dia, c[10], estado)
    }

    /** Días que tiene el mes (con febrero bisiesto): un 31 de abril no es una fecha real. */
    fun diasDelMes(anio: Int, mes: Int): Int = when (mes) {
        2 -> if ((anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
}
