package com.pame.karsy.data.repository

import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.CuentaAdminDto
import com.pame.karsy.data.remote.DestacadoAdminDto
import com.pame.karsy.data.remote.ReporteAdminDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Operaciones del panel de administración. Las escrituras pasan por funciones
 * admin_* de la BD, que validan el rol y registran la acción en la bitácora.
 */
object AdminRepository {

    private val db get() = Supabase.client

    suspend fun accounts(): List<CuentaAdminDto> =
        db.postgrest.rpc("admin_listar_cuentas").decodeList<CuentaAdminDto>()

    /** Todos los anuncios (el admin ve también pendientes y deshabilitados). */
    suspend fun cars(): List<Pair<Car, AnuncioDto>> =
        db.from("v_anuncios").select { order("fecha_creacion", Order.DESCENDING) }
            .decodeList<AnuncioDto>()
            .map { CarRepository.toCar(it) to it }

    suspend fun reports(): List<ReporteAdminDto> =
        db.postgrest.rpc("admin_listar_reportes").decodeList<ReporteAdminDto>()

    suspend fun featuredRequests(): List<DestacadoAdminDto> =
        db.postgrest.rpc("admin_listar_destacados").decodeList<DestacadoAdminDto>()

    suspend fun moderateProposal(idPropuesta: Long, approve: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_moderar_propuesta", buildJsonObject {
            put("p_id_propuesta", idPropuesta)
            put("p_aprobar", approve)
            put("p_motivo", reason)
        })
    }

    suspend fun setCarEnabled(idPublicacion: Long, enabled: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_cambiar_estado_publicacion", buildJsonObject {
            put("p_id_publicacion", idPublicacion)
            put("p_habilitar", enabled)
            put("p_motivo", reason)
        })
    }

    suspend fun setAccountSuspended(idCuenta: String, suspended: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_cambiar_estado_cuenta", buildJsonObject {
            put("p_id_cuenta", idCuenta)
            put("p_suspender", suspended)
            put("p_motivo", reason)
        })
    }

    /** estado: "atendido" | "descartado". */
    suspend fun resolveReport(idReporte: Long, estado: String, reason: String, disableCar: Boolean) {
        db.postgrest.rpc("admin_resolver_reporte", buildJsonObject {
            put("p_id_reporte", idReporte)
            put("p_estado", estado)
            put("p_motivo", reason)
            put("p_deshabilitar", disableCar)
        })
    }

    suspend fun resolveFeatured(idSolicitud: Long, approve: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_resolver_destacado", buildJsonObject {
            put("p_id_solicitud", idSolicitud)
            put("p_aprobar", approve)
            put("p_motivo", reason)
        })
    }
}
