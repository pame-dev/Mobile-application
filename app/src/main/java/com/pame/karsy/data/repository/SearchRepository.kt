package com.pame.karsy.data.repository

import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.data.remote.BusquedaGuardadaDto
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Filtros de una búsqueda a guardar (ids del catálogo); null = cualquiera. */
data class FiltrosBusqueda(
    val idMarca: Long? = null,
    val idModelo: Long? = null,
    val anio: Int? = null,
    val idCarroceria: Long? = null,
    val precioMin: Double? = null,
    val precioMax: Double? = null,
    val estado: String? = null,
) {
    val vacia: Boolean
        get() = listOf(idMarca, idModelo, anio, idCarroceria, precioMin, precioMax, estado).all { it == null }
}

/**
 * Búsquedas guardadas (Configuración → Búsquedas guardadas). Cuando se publica un auto
 * que coincide, la BD crea el aviso "busqueda_coincidencia" (también llega por push).
 */
object SearchRepository {

    private val db get() = Supabase.client

    suspend fun mine(): List<BusquedaGuardadaDto> {
        val uid = SessionManager.userId ?: return emptyList()
        return db.from("busquedas_guardadas").select(
            Columns.raw("*, marcas(nombre_marca), modelos(nombre_modelo), carrocerias(nombre_carroceria)")
        ) {
            filter { eq("id_cuenta", uid) }
            order("fecha_creacion", Order.DESCENDING)
        }.decodeList<BusquedaGuardadaDto>()
    }

    suspend fun save(f: FiltrosBusqueda) {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.saved_search_login))
        try {
            db.from("busquedas_guardadas").insert(buildJsonObject {
                put("id_cuenta", uid)
                put("id_marca", f.idMarca)
                put("id_modelo", f.idModelo)
                put("anio", f.anio)
                put("id_carroceria", f.idCarroceria)
                put("precio_min", f.precioMin)
                put("precio_max", f.precioMax)
                put("estado_ubicacion", f.estado)
            })
        } catch (e: PostgrestRestException) {
            // 23505: uq_busquedas_guardadas (la misma búsqueda ya estaba guardada).
            if (e.code == "23505") throw UserFacingException(texto(R.string.saved_search_duplicate))
            throw e
        }
    }

    suspend fun delete(id: Long) {
        db.from("busquedas_guardadas").delete { filter { eq("id_busqueda", id) } }
    }

    /** "Toyota Hilux · 2019 · Pickup · Hasta $500,000 · Coahuila". */
    fun describir(b: BusquedaGuardadaDto): String = listOfNotNull(
        listOfNotNull(b.marcas?.nombre, b.modelos?.nombre).joinToString(" ").ifBlank { null },
        b.anio?.toString(),
        b.carrocerias?.nombre,
        when {
            b.precioMin != null && b.precioMax != null -> "${Formato.precio(b.precioMin)} – ${Formato.precio(b.precioMax)}"
            b.precioMax != null -> texto(R.string.saved_search_price_max, Formato.precio(b.precioMax))
            b.precioMin != null -> texto(R.string.saved_search_price_min, Formato.precio(b.precioMin))
            else -> null
        },
        b.estado,
    ).joinToString(" · ")
}
