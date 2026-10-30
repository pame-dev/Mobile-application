package com.pame.karsy.data.repository

import com.pame.karsy.core.supabase.Supabase
import io.github.jan.supabase.storage.storage
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.CuentaAdminDto
import com.pame.karsy.data.remote.DestacadoAdminDto
import com.pame.karsy.data.remote.ReporteAdminDto
import com.pame.karsy.data.remote.VerificacionAdminDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Duration.Companion.minutes

/**
 * Operaciones del panel de administración. Las escrituras pasan por funciones
 * admin_* de la BD, que validan el rol y registran la acción en la bitácora.
 */
object AdminRepository {

    private val db get() = Supabase.client

    /** Lo que dura la URL firmada para ver un documento de verificación o un comprobante de pago. */
    private val CONSTANCIA_URL_EXPIRA = 10.minutes

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
        if (approve) borrarFotosRetiradas()
    }

    /**
     * Borra de Storage las fotos que ya nadie usa (p. ej. la original con placa que una
     * edición aprobada reemplazó por la tapada). Si falla, la aprobación ya quedó: se
     * reintenta en la siguiente.
     */
    private suspend fun borrarFotosRetiradas() {
        runCatching {
            val rutas = db.postgrest.rpc("admin_fotos_retiradas_sin_uso").decodeAs<List<String>>()
            if (rutas.isNotEmpty()) db.storage.from(Supabase.BUCKET).delete(rutas)
        }
    }

    /** El admin abrió el filtro "Deshabilitado" o "Pausado": dejan de salir en "Requiere atención". */
    suspend fun marcarVistas(estado: String) {
        db.postgrest.rpc("admin_marcar_vistas", buildJsonObject { put("p_estado", estado) })
    }

    /** El admin abrió el filtro "Suspendido" en Usuarios: dejan de salir en "Requiere atención". */
    suspend fun marcarSuspendidasVistas() {
        db.postgrest.rpc("admin_marcar_suspendidas_vistas")
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

    /** Documentos de verificación de todos los usuarios (pendientes primero). */
    suspend fun verifications(): List<VerificacionAdminDto> =
        db.postgrest.rpc("admin_listar_verificaciones_usuario").decodeList<VerificacionAdminDto>()

    /** Aprueba el documento [id] (marca cuentas.verificado) o lo rechaza con [reason] (mín. 10 caracteres). */
    suspend fun resolveVerification(id: String, approve: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_resolver_verificacion_usuario", buildJsonObject {
            put("p_id", id)
            put("p_aprobar", approve)
            put("p_motivo", reason)
        })
        borrarDocumentosSinUso()
    }

    /**
     * Borra del bucket privado los documentos de verificación que ya ningún registro usa
     * (reenvíos viejos y repetidos). Nunca lanza: si falla, se reintenta al abrir el panel
     * o en la siguiente revisión.
     */
    suspend fun borrarDocumentosSinUso() {
        runCatching {
            val rutas = db.postgrest.rpc("admin_documentos_verificacion_sin_uso").decodeAs<List<String>>()
            if (rutas.isNotEmpty()) db.storage.from(UserRepository.BUCKET_CONSTANCIAS).delete(rutas)
        }
    }

    /** URL firmada y temporal de un documento del bucket privado (nunca una URL pública). */
    suspend fun constanciaSignedUrl(ruta: String): String =
        db.storage.from(UserRepository.BUCKET_CONSTANCIAS).createSignedUrl(ruta, CONSTANCIA_URL_EXPIRA)

    suspend fun resolveFeatured(idSolicitud: Long, approve: Boolean, reason: String? = null) {
        db.postgrest.rpc("admin_resolver_destacado", buildJsonObject {
            put("p_id_solicitud", idSolicitud)
            put("p_aprobar", approve)
            put("p_motivo", reason)
        })
    }

    /** Guarda precio, días y la cuenta (banco y CLABE) donde se pagan los destacados. */
    suspend fun guardarConfigPagos(precio: Double, dias: Int, banco: String?, clabe: String?) {
        db.postgrest.rpc("admin_guardar_config_pagos", buildJsonObject {
            put("p_precio", precio)
            put("p_dias", dias)
            put("p_banco", banco)
            put("p_clabe", clabe)
        })
    }

    /** URL firmada y temporal (10 min) del comprobante de pago de un destacado. */
    suspend fun comprobanteSignedUrl(ruta: String): String =
        db.storage.from(CarRepository.BUCKET_COMPROBANTES).createSignedUrl(ruta, CONSTANCIA_URL_EXPIRA)

    /**
     * Borra del bucket privado los comprobantes que ya no se necesitan (destacado terminado,
     * rechazado y reemplazado, o subidos y nunca enviados). Nunca lanza: si falla, se reintenta
     * al abrir el panel otra vez.
     */
    suspend fun borrarComprobantesSinUso() {
        runCatching {
            val rutas = db.postgrest.rpc("admin_comprobantes_sin_uso").decodeAs<List<String>>()
            if (rutas.isNotEmpty()) db.storage.from(CarRepository.BUCKET_COMPROBANTES).delete(rutas)
        }
    }
}
