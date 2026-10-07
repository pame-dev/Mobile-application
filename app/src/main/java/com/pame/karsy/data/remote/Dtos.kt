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
    @SerialName("id_modelo") val idModelo: Long? = null,
    val modelo: String? = null,
    val anio: Int? = null,
    val kilometraje: Int? = null,
    @SerialName("id_color") val idColor: Long? = null,
    val color: String? = null,
    @SerialName("id_transmision") val idTransmision: Long? = null,
    val transmision: String? = null,
    @SerialName("id_carroceria") val idCarroceria: Long? = null,
    val carroceria: String? = null,
    @SerialName("numero_propietarios_anteriores") val propietariosAnteriores: Int? = null,
    @SerialName("tiene_problemas") val tieneProblemas: Boolean? = null,
    @SerialName("descripcion_problemas") val descripcionProblemas: String? = null,
    @SerialName("recuperado_por_seguro") val recuperadoPorSeguro: Boolean? = null,
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
    /** Comentario del admin al deshabilitar; null si está habilitada. */
    @SerialName("motivo_deshabilitacion") val motivoDeshabilitacion: String? = null,
    val vendido: Boolean = false,
    /** Dónde se vendió: "dentro_app" o "fuera_app"; null si no está vendida. */
    @SerialName("medio_venta") val medioVenta: String? = null,
    val destacado: Boolean = false,
    @SerialName("destacado_pendiente") val destacadoPendiente: Boolean = false,
    val fotos: List<String> = emptyList(),
)

/** Fila de favoritos_mis_publicaciones(). */
@Serializable
data class FavoritosConteoDto(
    @SerialName("id_publicacion") val idPublicacion: Long,
    val total: Int,
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
    @SerialName("medio_contacto_principal") val medioContactoPrincipal: String? = null,
    @SerialName("foto_portada") val fotoPortada: String? = null,
)

/** Dirección del lote (perfiles_lote); nombre, descripción, logo y ubicación están en cuentas. */
@Serializable
data class PerfilLoteDto(
    @SerialName("id_cuenta") val idCuenta: String,
    val calle: String = "",
    val numero: String = "",
    val colonia: String = "",
    @SerialName("codigo_postal") val codigoPostal: String = "",
)

/** Solo la portada de una cuenta (UserRepository.coverOf). */
@Serializable
data class PortadaDto(@SerialName("foto_portada") val fotoPortada: String? = null)

/** Columnas públicas de public.cuentas que usa el directorio de lotes. */
@Serializable
data class LoteCuentaDto(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("nombre_mostrar") val nombreMostrar: String,
    @SerialName("foto_perfil") val fotoPerfil: String? = null,
    @SerialName("descripcion_corta") val descripcionCorta: String? = null,
    @SerialName("estado_perfil_ubi") val estado: String,
    @SerialName("municipio_perfil_ubi") val municipio: String,
)

/** Fila completa de public.perfiles_lote (dirección y horario). */
@Serializable
data class LotePerfilDto(
    @SerialName("id_cuenta") val idCuenta: String,
    val calle: String = "",
    val numero: String = "",
    val colonia: String = "",
    @SerialName("codigo_postal") val codigoPostal: String = "",
    @SerialName("horarios_atencion") val horarios: String? = null,
)

@Serializable
data class TelefonoDto(
    @SerialName("id_telefono") val idTelefono: Long,
    @SerialName("numero_telefono") val numero: String,
    @SerialName("permite_llamadas") val permiteLlamadas: Boolean = true,
    @SerialName("es_whatsapp") val esWhatsapp: Boolean = false,
    @SerialName("es_principal") val esPrincipal: Boolean,
    val activo: Boolean,
)

@Serializable
data class TelefonoInsert(
    @SerialName("id_cuenta") val idCuenta: String,
    @SerialName("numero_telefono") val numero: String,
    @SerialName("permite_llamadas") val permiteLlamadas: Boolean,
    @SerialName("es_whatsapp") val esWhatsapp: Boolean,
    @SerialName("es_principal") val esPrincipal: Boolean,
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

/** Fin de un destacado aprobado (solicitudes_destacado). */
@Serializable
data class DestacadoFinDto(
    @SerialName("id_publicacion") val idPublicacion: Long,
    @SerialName("fecha_fin_destacado") val fechaFin: String? = null,
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
    /** El número de [telefono] también es WhatsApp. */
    val whatsapp: Boolean? = null,
    @SerialName("numero_whatsapp") val numeroWhatsapp: String? = null,
    @SerialName("medio_principal") val medioPrincipal: String? = null,
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

/** Fila de public.notificaciones (bandeja del usuario, la llenan triggers). */
@Serializable
data class NotificacionDto(
    @SerialName("id_notificacion") val id: Long,
    val tipo: String,
    @SerialName("id_publicacion") val idPublicacion: Long? = null,
    @SerialName("titulo_vehiculo") val tituloVehiculo: String? = null,
    /** Comentario del admin (rechazo / deshabilitación); null si no aplica. */
    val motivo: String? = null,
    val leida: Boolean = false,
    @SerialName("fecha_creacion") val fecha: String,
)
