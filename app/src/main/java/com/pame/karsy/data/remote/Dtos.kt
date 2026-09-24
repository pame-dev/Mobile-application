package com.pame.karsy.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Filas tal como las devuelve Supabase (nombres de columna en snake_case).

/** Fila de la vista public.v_anuncios. */
@Serializable
data class AnuncioDto(
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("id_propietario") val idPropietario: String,
    @SerialName("propietario_nombre") val propietarioNombre: String,
    @SerialName("propietario_tipo") val propietarioTipo: String,
    @SerialName("propietario_foto") val propietarioFoto: String? = null,
    val titulo: String? = null,
    val descripcion: String? = null,
    val precio: Double? = null,
    val moneda: String? = null,
    @SerialName("estado_ubicacion") val estadoUbicacion: String? = null,
    @SerialName("municipio_ubicacion") val municipioUbicacion: String? = null,
    @SerialName("id_marca") val idMarca: Long? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val anio: Int? = null,
    val kilometraje: Int? = null,
    val color: String? = null,
    val transmision: String? = null,
    @SerialName("id_carroceria") val idCarroceria: Long? = null,
    val carroceria: String? = null,
    @SerialName("numero_propietarios_anteriores") val propietariosAnteriores: Int? = null,
    @SerialName("tiene_problemas") val tieneProblemas: Boolean? = null,
    @SerialName("descripcion_problemas") val descripcionProblemas: String? = null,
    @SerialName("tipo_combustible") val combustible: String? = null,
    val cilindros: Int? = null,
    val cilindrada: String? = null,
    @SerialName("estado_publicacion") val estadoPublicacion: String,
    @SerialName("estado_administrativo") val estadoAdministrativo: String,
    @SerialName("fecha_creacion") val fechaCreacion: String,
    @SerialName("fecha_primera_publicacion") val fechaPrimeraPublicacion: String? = null,
    val aprobada: Boolean = false,
    val visible: Boolean = false,
    @SerialName("id_ultima_propuesta") val idUltimaPropuesta: Long? = null,
    @SerialName("estado_ultima_propuesta") val estadoUltimaPropuesta: String? = null,
    @SerialName("motivo_rechazo_propuesta") val motivoRechazo: String? = null,
    val destacado: Boolean = false,
    @SerialName("destacado_pendiente") val destacadoPendiente: Boolean = false,
    val fotos: List<String> = emptyList(),
)

@Serializable
data class CuentaDto(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("tipo_cuenta") val tipoCuenta: String,
    @SerialName("nombre_mostrar") val nombreMostrar: String,
    @SerialName("foto_perfil") val fotoPerfil: String? = null,
    @SerialName("descripcion_corta") val descripcionCorta: String? = null,
    @SerialName("estado_perfil_ubi") val estado: String,
    @SerialName("municipio_perfil_ubi") val municipio: String,
    @SerialName("estado_cuenta") val estadoCuenta: String,
    @SerialName("fecha_creacion") val fechaCreacion: String,
)

@Serializable
data class PerfilLoteDto(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("nombre_comercial") val nombreComercial: String,
    @SerialName("descripcion_lote") val descripcionLote: String? = null,
    val municipio: String,
    val estado: String,
)

@Serializable
data class TelefonoDto(
    @SerialName("id_telefono") val idTelefono: Long,
    @SerialName("numero_telefono") val numero: String,
    @SerialName("es_principal") val esPrincipal: Boolean,
    val activo: Boolean,
)

@Serializable
data class TelefonoInsert(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("numero_telefono") val numero: String,
    @SerialName("es_principal") val esPrincipal: Boolean = true,
    @SerialName("es_whatsapp") val esWhatsapp: Boolean = true,
)

@Serializable
data class FavoritoDto(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("id_publicacion") val idPublicacion: Long,
)

@Serializable
data class ReporteInsert(
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("id_reportante") val idReportante: String,
    @SerialName("motivo_reporte") val motivo: String,
)

@Serializable
data class SolicitudDestacadoInsert(
    @SerialName("id_publicacion") val idPublicacion: Long,
)

@Serializable
data class InteraccionInsert(
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("id_cuenta") val idCuenta: String?,
    @SerialName("tipo_interaccion") val tipo: String,
)

@Serializable
data class InteraccionDto(
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("fecha_hora") val fechaHora: String,
)

/** Resultado de la función contacto_vendedor(). */
@Serializable
data class ContactoDto(
    @SerialName("id_cuenta") val idCuenta: String,
    val nombre: String,
    @SerialName("tipo_cuenta") val tipoCuenta: String,
    val descripcion: String? = null,
    val ubicacion: String? = null,
    @SerialName("foto_perfil") val fotoPerfil: String? = null,
    @SerialName("miembro_desde") val miembroDesde: String? = null,
    val telefono: String? = null,
    val whatsapp: Boolean? = null,
    val correo: String? = null,
)

// ── Catálogos ────────────────────────────────────────────────────────────────

@Serializable
data class MarcaDto(
    @SerialName("id_marca") val id: Long,
    @SerialName("nombre_marca") val nombre: String,
    @SerialName("es_otro") val esOtro: Boolean,
)

@Serializable
data class ModeloDto(
    @SerialName("id_modelo") val id: Long,
    @SerialName("id_marca") val idMarca: Long,
    @SerialName("nombre_modelo") val nombre: String,
    @SerialName("es_otro") val esOtro: Boolean,
)

@Serializable
data class ColorDto(
    @SerialName("id_color") val id: Long,
    @SerialName("nombre_color") val nombre: String,
    @SerialName("es_otro") val esOtro: Boolean,
)

@Serializable
data class TransmisionDto(
    @SerialName("id_transmision") val id: Long,
    @SerialName("nombre_transmision") val nombre: String,
    @SerialName("es_otro") val esOtro: Boolean,
)

@Serializable
data class CarroceriaDto(
    @SerialName("id_carroceria") val id: Long,
    @SerialName("nombre_carroceria") val nombre: String,
    @SerialName("es_otro") val esOtro: Boolean,
)

// ── Administración ───────────────────────────────────────────────────────────

@Serializable
data class CuentaAdminDto(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("nombre_mostrar") val nombre: String,
    val correo: String,
    @SerialName("tipo_cuenta") val tipoCuenta: String,
    @SerialName("estado_cuenta") val estadoCuenta: String,
    @SerialName("fecha_creacion") val fechaCreacion: String,
    @SerialName("es_admin") val esAdmin: Boolean,
    val municipio: String,
    val estado: String,
    val responsable: String,
    val publicaciones: Int,
    val activas: Int,
)

@Serializable
data class ReporteAdminDto(
    @SerialName("id_reporte") val idReporte: Long,
    @SerialName("id_publicacion") val idPublicacion: Long,
    val publicacion: String,
    val reportante: String,
    @SerialName("motivo_reporte") val motivo: String,
    @SerialName("estado_reporte") val estado: String,
    @SerialName("motivo_resolucion") val motivoResolucion: String? = null,
    @SerialName("fecha_reporte") val fecha: String,
)

@Serializable
data class DestacadoAdminDto(
    @SerialName("id_solicitud_destacado") val idSolicitud: Long,
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("estado_solicitud") val estado: String,
    @SerialName("fecha_solicitud") val fecha: String,
    @SerialName("motivo_rechazo") val motivoRechazo: String? = null,
    val solicitante: String,
    @SerialName("solicitante_foto") val solicitanteFoto: String? = null,
)
