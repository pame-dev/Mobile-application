package com.pame.karsy.data.model

/**
 * Perfil de la cuenta en sesión.
 * Se arma desde public.cuentas (+ perfiles_lote si es lote, + teléfono principal y correo).
 */
data class User(
    val id: String,
    val displayName: String,
    val email: String,
    val phone: String,
    val location: String,
    val bio: String,
    val avatarUrl: String?,
    /** Foto de portada del perfil; null = degradado de la marca. */
    val coverUrl: String? = null,
    val memberSince: String,
    val accountType: String, // "particular" | "lote"
    // Ubicación del perfil (location es "municipio, estado" para mostrar).
    val estado: String = "",
    val municipio: String = "",
    // Solo lote: responsable y dirección del lote.
    val responsable: String = "",
    val calle: String = "",
    val numero: String = "",
    val colonia: String = "",
    val codigoPostal: String = "",
    /** Número de WhatsApp ([phone] es el de llamadas); pueden ser el mismo. */
    val whatsapp: String = "",
    /** Cómo prefiere que lo contacten: "llamada" o "whatsapp". */
    val medioPrincipal: String = "llamada",
    /** cuentas.verificado: un admin aprobó su documento (insignia). */
    val verificado: Boolean = false,
    /** Estado del último documento enviado (verificaciones_usuario.estado) o Constancia.SIN_DOCUMENTO. */
    val estadoVerificacion: String = "sin_documento",
    /** Comentario del admin si rechazó ese último documento. */
    val motivoRechazoVerificacion: String? = null,
    /** verificaciones_usuario.fecha_envio de ese último documento (ISO); null si no hay. */
    val fechaEnvioVerificacion: String? = null,
    /** verificaciones_usuario.fecha_revision de ese último documento (ISO); null si no se ha revisado. */
    val fechaRevisionVerificacion: String? = null,
)
