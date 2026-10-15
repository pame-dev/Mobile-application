package com.pame.karsy.core.util

import com.pame.karsy.core.locale.Idioma

/**
 * Valores de catálogo (colores, transmisiones, carrocerías, combustibles, tracción): se
 * guardan en español en la BD y aquí solo se traducen para mostrarlos en inglés. Es el
 * mismo diccionario que web/src/lib/catalogo.ts. Si se agrega un valor nuevo al catálogo,
 * hay que agregar su traducción aquí; mientras tanto se muestra tal cual.
 */
object Catalogo {
    private val EN = mapOf(
        // Colores
        "Blanco" to "White", "Negro" to "Black", "Gris" to "Gray", "Plata" to "Silver", "Rojo" to "Red",
        "Azul" to "Blue", "Verde" to "Green", "Beige" to "Beige", "Café" to "Brown", "Amarillo" to "Yellow",
        "Naranja" to "Orange", "Vino" to "Burgundy",
        // Transmisiones
        "Manual" to "Manual", "Automática" to "Automatic", "CVT" to "CVT", "Doble embrague" to "Dual-clutch",
        // Carrocerías
        "Sedán" to "Sedan", "Hatchback" to "Hatchback", "SUV" to "SUV", "Pickup" to "Pickup", "Coupé" to "Coupe",
        "Minivan" to "Minivan", "Convertible" to "Convertible", "Van" to "Van",
        // Combustibles
        "Gasolina" to "Gasoline", "Diésel" to "Diesel", "Híbrido" to "Hybrid", "Eléctrico" to "Electric",
        // Tracción
        "Delantera" to "Front-wheel drive", "Trasera" to "Rear-wheel drive", "4x4" to "4x4",
        "Integral (AWD)" to "All-wheel drive (AWD)",
        // Opción "otro" de cada catálogo
        "Otros" to "Other",
    )

    /** Valor de catálogo en el idioma de la app (marcas, modelos y textos escritos a mano se muestran igual). */
    fun nombre(valor: String): String =
        if (Idioma.actual(Idioma.contextoActual()) == Idioma.EN) EN[valor.trim()] ?: valor else valor
}
