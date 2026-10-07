package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.data.model.Lot
import com.pame.karsy.data.model.LotContact
import com.pame.karsy.data.remote.LoteCuentaDto
import com.pame.karsy.data.remote.LotePerfilDto
import com.pame.karsy.data.remote.TelefonoDto
import io.github.jan.supabase.postgrest.from

/**
 * Directorio de lotes y agencias. Lee public.cuentas y public.perfiles_lote, que son
 * públicas; el inventario sale de las publicaciones visibles (CarRepository).
 */
object LotRepository {

    private val db get() = Supabase.client

    /**
     * Lotes con cuenta activa, de los que tienen más vehículos a los que tienen menos.
     * No incluye el lote de la cuenta en sesión: el directorio es para ver a los demás.
     */
    suspend fun lots(): List<Lot> {
        val propio = SessionManager.userId
        val cuentas = db.from("cuentas").select {
            filter {
                eq("tipo_cuenta", "lote")
                eq("estado_cuenta", "activa")
            }
        }.decodeList<LoteCuentaDto>()
        val perfiles = db.from("perfiles_lote").select().decodeList<LotePerfilDto>().associateBy { it.idCuenta }
        val conteo = CarRepository.visibleCars().groupingBy { it.ownerId }.eachCount()
        return cuentas
            .filter { it.idCuenta != propio }
            .map { toLot(it, perfiles[it.idCuenta], conteo[it.idCuenta] ?: 0) }
            .sortedWith(compareByDescending<Lot> { it.vehicleCount }.thenBy { it.name.lowercase() })
    }

    suspend fun lot(id: String): Lot? {
        val cuenta = db.from("cuentas").select {
            filter {
                eq("id_cuenta", id)
                eq("tipo_cuenta", "lote")
            }
        }.decodeSingleOrNull<LoteCuentaDto>() ?: return null
        val perfil = db.from("perfiles_lote").select { filter { eq("id_cuenta", id) } }
            .decodeSingleOrNull<LotePerfilDto>()
        return toLot(cuenta, perfil, vehicleCount = 0)
    }

    /**
     * Teléfono para llamar y número de WhatsApp del lote. Igual que en el detalle de
     * una publicación (contacto_vendedor), solo se entregan con sesión iniciada.
     */
    suspend fun contact(id: String): LotContact? {
        if (SessionManager.userId == null) return null
        val telefonos = db.from("telefonos_contacto").select {
            filter {
                eq("id_cuenta", id)
                eq("activo", true)
            }
        }.decodeList<TelefonoDto>().sortedByDescending { it.esPrincipal }
        return LotContact(
            phone = telefonos.firstOrNull { it.permiteLlamadas }?.numero,
            whatsapp = telefonos.firstOrNull { it.esWhatsapp }?.numero,
        )
    }

    private fun toLot(c: LoteCuentaDto, p: LotePerfilDto?, vehicleCount: Int): Lot {
        val municipio = p?.municipio ?: c.municipio
        val estado = p?.estado ?: c.estado
        val direccion = p?.let {
            listOf("${it.calle} ${it.numero}".trim(), it.colonia, "C.P. ${it.codigoPostal}", it.municipio, it.estado)
                .filter { parte -> parte.isNotBlank() && parte != "C.P. " }
                .joinToString(", ")
        }.orEmpty()
        return Lot(
            id = c.idCuenta,
            name = p?.nombreComercial?.takeIf { it.isNotBlank() } ?: c.nombreMostrar,
            logoUrl = Supabase.publicUrl(p?.logoLote) ?: Supabase.publicUrl(c.fotoPerfil),
            description = p?.descripcionLote?.takeIf { it.isNotBlank() } ?: c.descripcionCorta.orEmpty(),
            city = "$municipio, $estado",
            address = direccion,
            hours = p?.horarios,
            vehicleCount = vehicleCount,
        )
    }
}
