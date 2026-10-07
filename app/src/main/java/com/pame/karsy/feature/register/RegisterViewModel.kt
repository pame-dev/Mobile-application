package com.pame.karsy.feature.register

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.FormRules
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.PasswordRules
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.AuthRepository
import com.pame.karsy.data.repository.EmailNotVerifiedException
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Formulario de registro (particular y lote).
 * Se exigen los campos que la BD necesita y un teléfono de contacto; la contraseña
 * debe cumplir PasswordRules y coincidir con la confirmación.
 */
class RegisterViewModel : ViewModel() {
    // Datos personales / del responsable
    var nombre by mutableStateOf("")
    var apellido by mutableStateOf("")
    var correo by mutableStateOf("")
    var ciudad by mutableStateOf("")
    var estado by mutableStateOf("")

    // Contacto
    var telefono by mutableStateOf("")
    /** "¿Usas este mismo número para WhatsApp?" Sí: el campo de WhatsApp muestra el teléfono. */
    var mismoWhatsapp by mutableStateOf(true)
    var whatsapp by mutableStateOf("")
    /** Método de contacto principal: [LLAMADA] o [WHATSAPP]. */
    var medioContacto by mutableStateOf<String?>(null)

    // Solo lote
    var nombreLote by mutableStateOf("")
    var calle by mutableStateOf("")
    var numero by mutableStateOf("")
    var colonia by mutableStateOf("")
    var codigoPostal by mutableStateOf("")
    var descripcion by mutableStateOf("")
    var logo by mutableStateOf<Uri?>(null)

    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var acceptedTerms by mutableStateOf(false)

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    /** Campos que el usuario ya dejó (se validan al salir de ellos). */
    private var touched by mutableStateOf<Set<String>>(emptySet())
    /** Después de presionar "Crear cuenta" se muestran los errores de todos los campos. */
    private var submitted by mutableStateOf(false)

    /** El usuario salió del campo: desde ahora se muestra su error (y se actualiza al corregirlo). */
    fun touch(field: String) {
        if (field !in touched) touched = touched + field
        when (field) {
            // "juan pérez" → "Juan Pérez" al salir del campo.
            "nombre" -> nombre = FormRules.capitalizarNombre(nombre)
            "apellido" -> apellido = FormRules.capitalizarNombre(apellido)
            "telefono" -> revisarTelefonoEnUso()
        }
    }

    // ── Avisos que no bloquean el registro ──

    /** "Este teléfono ya está registrado en otra cuenta." */
    var telefonoEnUsoAviso by mutableStateOf<String?>(null)
        private set

    private fun revisarTelefonoEnUso() {
        val numero = FormRules.normalizarTelefono(telefono)
        telefonoEnUsoAviso = null
        if (FormRules.telefono(telefono) != null) return
        viewModelScope.launch {
            val enUso = safeCall { AuthRepository.telefonoEnUso(numero) }.getOrDefault(false)
            // Solo si sigue siendo el mismo número (pudo cambiar mientras se consultaba).
            if (enUso && FormRules.normalizarTelefono(telefono) == numero) {
                telefonoEnUsoAviso = texto(R.string.form_warning_phone_in_use)
            }
        }
    }

    /** "Este código postal es de Jalisco, no de Coahuila." */
    val codigoPostalAviso: String?
        get() = if (submitted || "codigoPostal" in touched) FormRules.avisoCodigoPostalEstado(codigoPostal.trim(), estado) else null

    /** El correo que Supabase dijo que ya tiene cuenta (se muestra con "Iniciar sesión"). */
    private var correoRegistrado by mutableStateOf<String?>(null)
    val correoYaRegistrado: Boolean
        get() = correoRegistrado != null && FormRules.limpiarCorreo(correo) == correoRegistrado

    /** Logo: solo se acepta si mide al menos LOGO_MIN_PX por lado. */
    var logoError by mutableStateOf<String?>(null)
        private set

    fun elegirLogo(context: Context, uri: Uri) {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) } }
        val lado = minOf(opciones.outWidth, opciones.outHeight)
        when {
            lado <= 0 -> logoError = texto(R.string.form_error_logo_invalid)
            lado < FormRules.LOGO_MIN_PX -> logoError = texto(R.string.form_error_logo_small, FormRules.LOGO_MIN_PX)
            else -> { logo = uri; logoError = null }
        }
    }

    /** La casilla de términos se marca en rojo si se intentó enviar sin aceptarlos. */
    val termsError: Boolean get() = submitted && !acceptedTerms

    /** Cambia cada vez que el envío falla: la pantalla lleva la vista al primer error. */
    var scrollToErrorRequest by mutableIntStateOf(0)
        private set

    /** Campos con error ahora (para llevar la vista al primero). */
    fun camposConError(esLote: Boolean): List<String> =
        camposDe(esLote).filter { validar(it) != null } +
            (if (correoYaRegistrado) listOf("correo") else emptyList()) +
            (if (password.isNotEmpty() && password != confirmPassword) listOf("confirmPassword") else emptyList()) +
            (if (!acceptedTerms) listOf("terminos") else emptyList())

    private fun camposDe(esLote: Boolean) =
        listOf("nombre", "apellido", "correo", "telefono", "whatsapp", "medio", "estado", "ciudad", "password") +
            if (esLote) listOf("nombreLote", "calle", "numero", "colonia", "codigoPostal") else emptyList()

    /** Corrección sugerida del correo ("gmil.com" → "gmail.com"), al salir del campo. */
    val correoSugerido: String?
        get() = if (submitted || "correo" in touched) FormRules.sugerenciaCorreo(correo) else null

    fun errorFor(field: String): String? =
        if (submitted || field in touched) validar(field) else null

    /** Error del campo con su valor actual, o null si está bien. */
    private fun validar(field: String): String? {
        val requerido = texto(R.string.auth_field_required)
        fun obligatorio(valor: String, regla: (String) -> String?) = if (valor.isBlank()) requerido else regla(valor)
        return when (field) {
            "nombre" -> obligatorio(nombre, FormRules::nombrePersona)
            "apellido" -> obligatorio(apellido, FormRules::nombrePersona)
            "correo" -> obligatorio(correo, FormRules::correo)
                ?: if (correoYaRegistrado) texto(R.string.core_error_email_exists) else null
            "telefono" -> obligatorio(telefono, FormRules::telefono)
            // Si no usa el mismo número, el WhatsApp es opcional (vacío = no tiene WhatsApp).
            "whatsapp" -> when {
                mismoWhatsapp -> null
                medioContacto == WHATSAPP && whatsapp.isBlank() -> texto(R.string.auth_error_whatsapp_or_calls)
                else -> FormRules.telefono(whatsapp)
            }
            "medio" -> if (medioContacto == null) texto(R.string.auth_error_choose_contact) else null
            "estado" -> if (estado.isBlank()) requerido else null
            "ciudad" -> if (ciudad.isBlank()) requerido else null
            "password" -> if (password.isEmpty()) requerido else PasswordRules.error(password, correo)
            "nombreLote" -> obligatorio(nombreLote, FormRules::nombreLote)
            "calle" -> obligatorio(calle, FormRules::textoDireccion)
            "numero" -> obligatorio(numero, FormRules::numeroExterior)
            "colonia" -> obligatorio(colonia, FormRules::textoDireccion)
            "codigoPostal" -> obligatorio(codigoPostal, FormRules::codigoPostal)
            else -> null
        }
    }

    val passwordMismatch: String?
        get() = if (confirmPassword.isNotEmpty() && password != confirmPassword) texto(R.string.auth_passwords_mismatch) else null

    fun submit(
        esLote: Boolean,
        context: Context,
        onCreated: (SessionAccount) -> Unit,
        onVerifyEmail: (String) -> Unit,
    ) {
        if (loading) return
        submitted = true
        nombre = FormRules.capitalizarNombre(nombre)
        apellido = FormRules.capitalizarNombre(apellido)
        val errores = camposDe(esLote).mapNotNull { validar(it) }
        val requerido = texto(R.string.auth_field_required)

        val numeroTelefono = FormRules.normalizarTelefono(telefono)
        val numeroWhatsapp = if (mismoWhatsapp) numeroTelefono else FormRules.normalizarTelefono(whatsapp)

        error = when {
            requerido in errores -> texto(R.string.auth_error_complete_required)
            errores.isNotEmpty() -> texto(R.string.auth_error_check_red)
            password != confirmPassword -> texto(R.string.auth_passwords_mismatch)
            !acceptedTerms -> texto(R.string.auth_error_accept_terms)
            else -> null
        }
        if (error != null) {
            scrollToErrorRequest++
            return
        }

        val nombreCompleto = "${FormRules.limpiarEspacios(nombre)} ${FormRules.limpiarEspacios(apellido)}"
        val datos = buildJsonObject {
            put("tipo_cuenta", if (esLote) "lote" else "particular")
            put("nombre_mostrar", if (esLote) FormRules.limpiarEspacios(nombreLote) else nombreCompleto)
            put("estado", estado.trim())
            put("municipio", ciudad.trim())
            // La BD guarda una sola fila si ambos números son iguales (ver fn_auth_crear_cuenta).
            put("telefono", numeroTelefono)
            put("whatsapp", numeroWhatsapp)
            put("medio_contacto", medioContacto)
            put("descripcion", descripcion.trim())
            if (esLote) {
                put("responsable", nombreCompleto)
                put("calle", FormRules.limpiarEspacios(calle))
                put("numero", FormRules.limpiarEspacios(numero))
                put("colonia", FormRules.limpiarEspacios(colonia))
                put("codigo_postal", codigoPostal.trim())
            }
        }

        val appContext = context.applicationContext
        loading = true
        viewModelScope.launch {
            safeCall { AuthRepository.signUp(FormRules.limpiarCorreo(correo), password, datos) }
                .onSuccess { cuenta ->
                    SessionManager.login(cuenta)
                    // El logo es opcional: si falla la subida la cuenta ya quedó creada.
                    logo?.let { uri ->
                        safeCall { UserRepository.uploadAvatar(Imagenes.jpegBytes(appContext, uri, maxLado = 800)) }
                    }
                    loading = false
                    onCreated(cuenta)
                }
                .onFailure { e ->
                    if (e is EmailNotVerifiedException) {
                        // Cuenta creada, pero sin sesión hasta escribir el código del correo:
                        // el logo se sube al verificarlo.
                        logo?.let { uri ->
                            safeCall { Imagenes.jpegBytes(appContext, uri, maxLado = 800) }
                                .onSuccess { UserRepository.holdAvatarUntilVerified(e.email, it) }
                        }
                        loading = false
                        onVerifyEmail(e.email)
                    } else {
                        error = e.mensajeUsuario()
                        // Correo ya registrado: se marca el campo y se ofrece iniciar sesión.
                        if (error == texto(R.string.core_error_email_exists)) {
                            correoRegistrado = FormRules.limpiarCorreo(correo)
                            scrollToErrorRequest++
                        }
                        loading = false
                    }
                }
        }
    }

    companion object {
        // Valores de cuentas.medio_contacto_principal.
        const val LLAMADA = "llamada"
        const val WHATSAPP = "whatsapp"
    }
}
