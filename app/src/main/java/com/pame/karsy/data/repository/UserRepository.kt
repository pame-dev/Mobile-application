package com.pame.karsy.data.repository

import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.data.model.User
import com.pame.karsy.data.remote.CuentaDto
import com.pame.karsy.data.remote.PerfilLoteDto
import com.pame.karsy.data.remote.PortadaDto
import com.pame.karsy.data.remote.TelefonoDto
import com.pame.karsy.data.remote.TelefonoInsert
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

/** Perfil de la cuenta en sesión (public.cuentas + perfiles_lote + teléfono + correo). */
object UserRepository {

    /** Valores de cuentas.medio_contacto_principal. */
    const val LLAMADA = "llamada"
    const val WHATSAPP = "whatsapp"

    private val db get() = Supabase.client

    suspend fun currentUser(): User? {
        val uid = SessionManager.userId ?: return null
        val cuenta = db.from("cuentas").select { filter { eq("id_cuenta", uid) } }
            .decodeSingleOrNull<CuentaDto>() ?: return null
        val lote = if (cuenta.tipoCuenta == "lote") {
            db.from("perfiles_lote").select { filter { eq("id_cuenta", uid) } }
                .decodeSingleOrNull<PerfilLoteDto>()
        } else null
        val telefonos = activePhones(uid)
        val telefono = telefonos.firstOrNull { it.permiteLlamadas }
        val whatsapp = telefonos.firstOrNull { it.esWhatsapp }
        val correo = SessionManager.account?.correo?.takeIf { it.isNotBlank() }
            ?: db.postgrest.rpc("mi_correo").decodeAsOrNull<String>().orEmpty()

        return User(
            id = cuenta.idCuenta,
            displayName = cuenta.nombreMostrar,
            email = correo,
            phone = telefono?.numero.orEmpty(),
            location = "${cuenta.municipio}, ${cuenta.estado}",
            bio = cuenta.descripcionCorta.orEmpty(),
            avatarUrl = Supabase.publicUrl(cuenta.fotoPerfil),
            coverUrl = Supabase.publicUrl(cuenta.fotoPortada),
            memberSince = Formato.mesAnio(cuenta.fechaCreacion),
            accountType = cuenta.tipoCuenta,
            estado = cuenta.estado,
            municipio = cuenta.municipio,
            // El responsable del lote se guardó en los metadatos de Auth al registrarse.
            responsable = db.auth.currentUserOrNull()?.userMetadata?.get("responsable")
                ?.jsonPrimitive?.contentOrNull.orEmpty(),
            calle = lote?.calle.orEmpty(),
            numero = lote?.numero.orEmpty(),
            colonia = lote?.colonia.orEmpty(),
            codigoPostal = lote?.codigoPostal.orEmpty(),
            whatsapp = whatsapp?.numero.orEmpty(),
            medioPrincipal = when {
                telefono == null -> WHATSAPP
                whatsapp == null -> LLAMADA
                else -> cuenta.medioContactoPrincipal ?: LLAMADA
            },
        )
    }

    /**
     * Guarda nombre, descripción, ubicación y teléfono; en un lote también su nombre
     * comercial, dirección y responsable. El correo no se cambia desde el perfil.
     */
    suspend fun updateProfile(original: User, updated: User) {
        val uid = updated.id
        db.from("cuentas").update({
            set("nombre_mostrar", updated.displayName.trim())
            set("descripcion_corta", updated.bio.trim().ifEmpty { null }?.take(300))
            set("estado_perfil_ubi", updated.estado.trim())
            set("municipio_perfil_ubi", updated.municipio.trim())
        }) { filter { eq("id_cuenta", uid) } }

        if (updated.accountType == "lote") {
            // Nombre, descripción y ubicación del lote son los de cuentas (arriba).
            db.from("perfiles_lote").update({
                set("calle", updated.calle.trim())
                set("numero", updated.numero.trim())
                set("colonia", updated.colonia.trim())
                set("codigo_postal", updated.codigoPostal.trim())
            }) { filter { eq("id_cuenta", uid) } }
            if (updated.responsable.trim() != original.responsable.trim()) {
                db.auth.updateUser { data { put("responsable", updated.responsable.trim()) } }
            }
        }

        if (updated.phone.trim() != original.phone.trim() ||
            updated.whatsapp.trim() != original.whatsapp.trim() ||
            updated.medioPrincipal != original.medioPrincipal
        ) savePhones(uid, updated.phone.trim(), updated.whatsapp.trim(), updated.medioPrincipal)
        SessionManager.updateName(updated.displayName.trim())
    }

    /** Foto elegida en el registro que espera a que se verifique el correo (antes no hay sesión). */
    private var pendingAvatar: Pair<String, ByteArray>? = null

    fun holdAvatarUntilVerified(email: String, bytes: ByteArray) {
        pendingAvatar = email.trim().lowercase() to bytes
    }

    /** Sube la foto guardada por [holdAvatarUntilVerified] si es de este correo. */
    suspend fun uploadPendingAvatar(email: String) {
        val (correo, bytes) = pendingAvatar ?: return
        pendingAvatar = null
        if (correo == email.trim().lowercase()) uploadAvatar(bytes)
    }

    /** Sube la foto de perfil (o logo del lote) y la guarda en la cuenta. Devuelve su URL. */
    suspend fun uploadAvatar(bytes: ByteArray): String? {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_first))
        val ruta = "perfiles/$uid/${UUID.randomUUID()}.jpg"
        db.storage.from(Supabase.BUCKET).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        // En un lote, la foto de perfil es su logo.
        db.from("cuentas").update({ set("foto_perfil", ruta) }) { filter { eq("id_cuenta", uid) } }
        return Supabase.publicUrl(ruta)
    }

    /** Sube la foto de portada (ya comprimida) y la guarda en la cuenta. Devuelve su URL. */
    suspend fun uploadCover(bytes: ByteArray): String? {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_first))
        val ruta = "perfiles/$uid/portada-${UUID.randomUUID()}.jpg"
        db.storage.from(Supabase.BUCKET).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        db.from("cuentas").update({ set("foto_portada", ruta) }) { filter { eq("id_cuenta", uid) } }
        return Supabase.publicUrl(ruta)
    }

    /** Quita la portada: el perfil vuelve al degradado de la marca. */
    suspend fun removeCover() {
        val uid = SessionManager.userId ?: return
        db.from("cuentas").update({ set("foto_portada", null as String?) }) { filter { eq("id_cuenta", uid) } }
    }

    /** Portada de cualquier cuenta (public.cuentas es de lectura pública). */
    suspend fun coverOf(accountId: String): String? =
        db.from("cuentas").select(Columns.list("foto_portada")) { filter { eq("id_cuenta", accountId) } }
            .decodeSingleOrNull<PortadaDto>()?.fotoPortada
            ?.let(Supabase::publicUrl)

    /**
     * Deja los teléfonos como los arma el registro (fn_auth_crear_cuenta): una fila si
     * llamadas y WhatsApp usan el mismo número, o una por número; es_principal marca el
     * del medio preferido, que también se guarda en cuentas.medio_contacto_principal.
     * Un número quitado se desactiva (no se borra) y se reactiva si vuelve a agregarse.
     */
    private suspend fun savePhones(uid: String, llamadas: String, whatsapp: String, medio: String) {
        data class Deseado(val numero: String, val llamadas: Boolean, val whatsapp: Boolean, val principal: Boolean)
        val deseados = if (llamadas.isNotEmpty() && llamadas == whatsapp) {
            listOf(Deseado(llamadas, llamadas = true, whatsapp = true, principal = true))
        } else listOfNotNull(
            Deseado(llamadas, llamadas = true, whatsapp = false, principal = medio == LLAMADA).takeIf { llamadas.isNotEmpty() },
            Deseado(whatsapp, llamadas = false, whatsapp = true, principal = medio == WHATSAPP).takeIf { whatsapp.isNotEmpty() },
        )
        // Primero lo que puede fallar por permisos, para no dejar los teléfonos a medias.
        db.from("cuentas").update({ set("medio_contacto_principal", medio) }) { filter { eq("id_cuenta", uid) } }
        val existentes = db.from("telefonos_contacto").select { filter { eq("id_cuenta", uid) } }.decodeList<TelefonoDto>()

        // Primero nadie es principal (uq_telefonos_contacto_principal) y se apagan los que ya no van.
        db.from("telefonos_contacto").update({ set("es_principal", false) }) { filter { eq("id_cuenta", uid) } }
        existentes.filter { e -> e.activo && deseados.none { it.numero == e.numero } }.forEach { e ->
            db.from("telefonos_contacto").update({ set("activo", false) }) { filter { eq("id_telefono", e.idTelefono) } }
        }
        // El principal al final, cuando los demás ya no lo son.
        deseados.sortedBy { it.principal }.forEach { d ->
            val fila = existentes.firstOrNull { it.numero == d.numero }
            if (fila != null) {
                db.from("telefonos_contacto").update({
                    set("permite_llamadas", d.llamadas)
                    set("es_whatsapp", d.whatsapp)
                    set("es_principal", d.principal)
                    set("activo", true)
                }) { filter { eq("id_telefono", fila.idTelefono) } }
            } else {
                db.from("telefonos_contacto").insert(TelefonoInsert(uid, d.numero, d.llamadas, d.whatsapp, d.principal))
            }
        }
    }

    private suspend fun activePhones(uid: String): List<TelefonoDto> =
        db.from("telefonos_contacto").select { filter { eq("id_cuenta", uid) } }
            .decodeList<TelefonoDto>()
            .filter { it.activo }
            .sortedByDescending { it.esPrincipal }
}
