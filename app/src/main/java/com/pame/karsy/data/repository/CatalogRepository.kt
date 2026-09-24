package com.pame.karsy.data.repository

import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.remote.CarroceriaDto
import com.pame.karsy.data.remote.ColorDto
import com.pame.karsy.data.remote.MarcaDto
import com.pame.karsy.data.remote.ModeloDto
import com.pame.karsy.data.remote.TransmisionDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

/** Catálogos de la BD (marcas, modelos, colores…). Se cargan una vez por sesión de la app. */
data class Catalogs(
    val marcas: List<MarcaDto>,
    val modelos: List<ModeloDto>,
    val colores: List<ColorDto>,
    val transmisiones: List<TransmisionDto>,
    val carrocerias: List<CarroceriaDto>,
) {
    fun modelosDe(idMarca: Long?): List<ModeloDto> = modelos.filter { it.idMarca == idMarca }
}

object CatalogRepository {

    private var cache: Catalogs? = null

    suspend fun get(): Catalogs {
        cache?.let { return it }
        val db = Supabase.client
        // "Otros" siempre al final de cada lista.
        val loaded = Catalogs(
            marcas = db.from("marcas").select { order("nombre_marca", Order.ASCENDING) }
                .decodeList<MarcaDto>().sortedBy { it.esOtro },
            modelos = db.from("modelos").select { order("nombre_modelo", Order.ASCENDING) }
                .decodeList<ModeloDto>().sortedBy { it.esOtro },
            colores = db.from("colores").select { order("id_color", Order.ASCENDING) }
                .decodeList<ColorDto>().sortedBy { it.esOtro },
            transmisiones = db.from("transmisiones").select { order("id_transmision", Order.ASCENDING) }
                .decodeList<TransmisionDto>().sortedBy { it.esOtro },
            carrocerias = db.from("carrocerias").select { order("id_carroceria", Order.ASCENDING) }
                .decodeList<CarroceriaDto>().sortedBy { it.esOtro },
        )
        cache = loaded
        return loaded
    }
}
