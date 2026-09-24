package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.Formato
import com.pame.karsy.data.model.User
import com.pame.karsy.data.remote.CuentaDto
import com.pame.karsy.data.remote.PerfilLoteDto
import com.pame.karsy.data.remote.TelefonoDto
import com.pame.karsy.data.remote.TelefonoInsert
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID

/** Perfil de la cuenta en sesión (public.cuentas + perfiles_lote + teléfono + correo). */
object UserRepository {

    private val db get() = Supabase.client

    suspend fun currentUser(): User? {
        val uid = SessionManager.userId ?: return null
        val cuenta = db.from("cuentas").select { filter { eq("id_cuenta", uid) } }
            .decodeSingleOrNull<CuentaDto>() ?: return null
        val lote = if (cuenta.tipoCuenta == "lote") {
            db.from("perfiles_lote").select { filter { eq("id_cuenta", uid) } }
                .decodeSingleOrNull<PerfilLoteDto>()
        } else null
        val telefono = principalPhone(uid)
        val correo = SessionManager.account?.correo?.takeIf { it.isNotBlank() }
            ?: db.postgrest.rpc("mi_correo").decodeAsOrNull<String>().orEmpty()

        return User(
            id = cuenta.idCuenta,
            displayName = cuenta.nombreMostrar,
            email = correo,
            phone = telefono?.numero.orEmpty(),
            location = "${cuenta.municipio}, ${cuenta.estado}",
            bio = cuenta.descripcionCorta ?: lote?.descripcionLote.orEmpty(),
            avatarUrl = Supabase.publicUrl(cuenta.fotoPerfil),
            memberSince = Formato.mesAnio(cuenta.fechaCreacion),
            accountType = cuenta.tipoCuenta,
        )
    }

    /**
     * Guarda nombre, descripción y teléfono. Si cambia el correo se pide a Supabase Auth
     * (puede requerir confirmarlo desde el correo nuevo).
     */
    suspend fun updateProfile(original: User, updated: User) {
        val uid = updated.id
        db.from("cuentas").update({
            set("nombre_mostrar", updated.displayName.trim())
            set("descripcion_corta", updated.bio.trim().ifEmpty { null }?.take(300))
        }) { filter { eq("id_cuenta", uid) } }

        if (updated.phone.trim() != original.phone.trim()) {
            val actual = principalPhone(uid)
            val nuevo = updated.phone.trim()
            when {
                nuevo.isEmpty() && actual != null ->
                    db.from("telefonos_contacto").update({ set("activo", false) }) {
                        filter { eq("id_telefono", actual.idTelefono) }
                    }
                nuevo.isNotEmpty() && actual != null ->
                    db.from("telefonos_contacto").update({ set("numero_telefono", nuevo) }) {
                        filter { eq("id_telefono", actual.idTelefono) }
                    }
                nuevo.isNotEmpty() ->
                    db.from("telefonos_contacto").insert(TelefonoInsert(uid, nuevo))
            }
        }

        if (updated.email.trim() != original.email.trim() && updated.email.isNotBlank()) {
            db.auth.updateUser { email = updated.email.trim() }
        }
        SessionManager.updateName(updated.displayName.trim())
    }

    /** Sube la foto de perfil (o logo del lote) y la guarda en la cuenta. Devuelve su URL. */
    suspend fun uploadAvatar(bytes: ByteArray): String? {
        val uid = SessionManager.userId ?: throw IllegalStateException("Inicia sesión primero.")
        val ruta = "perfiles/$uid/${UUID.randomUUID()}.jpg"
        db.storage.from(Supabase.BUCKET).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        db.from("cuentas").update({ set("foto_perfil", ruta) }) { filter { eq("id_cuenta", uid) } }
        if (SessionManager.account?.tipoCuenta == "lote") {
            db.from("perfiles_lote").update({ set("logo_lote", ruta) }) { filter { eq("id_cuenta", uid) } }
        }
        return Supabase.publicUrl(ruta)
    }

    private suspend fun principalPhone(uid: String): TelefonoDto? =
        db.from("telefonos_contacto").select { filter { eq("id_cuenta", uid) } }
            .decodeList<TelefonoDto>()
            .filter { it.activo }
            .sortedByDescending { it.esPrincipal }
            .firstOrNull()
}
