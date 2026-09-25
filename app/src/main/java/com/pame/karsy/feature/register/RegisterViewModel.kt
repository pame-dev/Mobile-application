package com.pame.karsy.feature.register

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Imagenes
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
 * solo debe coincidir con la confirmación (el mínimo de 6 caracteres lo marca el servidor).
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
    /** Campos obligatorios vacíos después de intentar enviar. */
    var missing by mutableStateOf<Set<String>>(emptySet())
        private set
    /** Campos llenos pero con un valor que no sirve (campo → mensaje). */
    private var invalid by mutableStateOf<Map<String, String>>(emptyMap())

    fun errorFor(field: String): String? = if (field in missing) "Campo obligatorio" else invalid[field]

    val passwordMismatch: String?
        get() = if (confirmPassword.isNotEmpty() && password != confirmPassword) "Las contraseñas no coinciden." else null

    fun submit(
        esLote: Boolean,
        context: Context,
        onCreated: (SessionAccount) -> Unit,
        onVerifyEmail: (String) -> Unit,
    ) {
        if (loading) return
        val requeridos = buildMap {
            put("nombre", nombre); put("apellido", apellido); put("correo", correo)
            put("telefono", telefono)
            put("ciudad", ciudad); put("estado", estado); put("password", password)
            if (esLote) {
                put("nombreLote", nombreLote); put("calle", calle); put("numero", numero)
                put("colonia", colonia); put("codigoPostal", codigoPostal)
            }
        }
        missing = requeridos.filterValues { it.isBlank() }.keys

        // Si no usa el mismo número, el WhatsApp es opcional (vacío = no tiene WhatsApp).
        val numeroTelefono = soloNumero(telefono)
        val numeroWhatsapp = if (mismoWhatsapp) numeroTelefono else soloNumero(whatsapp)
        invalid = buildMap {
            if (telefono.isNotBlank() && !esNumeroValido(numeroTelefono)) put("telefono", "Escribe un número de 10 dígitos.")
            if (!mismoWhatsapp && whatsapp.isNotBlank() && !esNumeroValido(numeroWhatsapp)) {
                put("whatsapp", "Escribe un número de 10 dígitos.")
            }
            when {
                medioContacto == null -> put("medio", "Elige cómo prefieres que te contacten.")
                medioContacto == WHATSAPP && numeroWhatsapp.isEmpty() ->
                    put("whatsapp", "Escribe tu WhatsApp o elige Llamadas como método principal.")
            }
        }

        error = when {
            missing.isNotEmpty() -> "Completa los campos obligatorios."
            invalid.isNotEmpty() -> "Revisa los datos marcados en rojo."
            password != confirmPassword -> "Las contraseñas no coinciden."
            !acceptedTerms -> "Acepta los términos y condiciones para continuar."
            else -> null
        }
        if (error != null) return

        val nombreCompleto = "${nombre.trim()} ${apellido.trim()}"
        val datos = buildJsonObject {
            put("tipo_cuenta", if (esLote) "lote" else "particular")
            put("nombre_mostrar", if (esLote) nombreLote.trim() else nombreCompleto)
            put("estado", estado.trim())
            put("municipio", ciudad.trim())
            // La BD guarda una sola fila si ambos números son iguales (ver fn_auth_crear_cuenta).
            put("telefono", numeroTelefono)
            put("whatsapp", numeroWhatsapp)
            put("medio_contacto", medioContacto)
            put("descripcion", descripcion.trim())
            if (esLote) {
                put("responsable", nombreCompleto)
                put("nombre_comercial", nombreLote.trim())
                put("calle", calle.trim())
                put("numero", numero.trim())
                put("colonia", colonia.trim())
                put("codigo_postal", codigoPostal.trim())
            }
        }

        val appContext = context.applicationContext
        loading = true
        viewModelScope.launch {
            safeCall { AuthRepository.signUp(correo, password, datos) }
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
                        loading = false
                    }
                }
        }
    }

    /** Quita espacios, guiones y paréntesis: "871 123-4567" -> "8711234567". */
    private fun soloNumero(texto: String) = texto.filter { it.isDigit() || it == '+' }

    /** 10 dígitos, o más si trae lada internacional (+52…). */
    private fun esNumeroValido(numero: String) = numero.count { it.isDigit() } >= 10

    companion object {
        // Valores de cuentas.medio_contacto_principal.
        const val LLAMADA = "llamada"
        const val WHATSAPP = "whatsapp"
    }
}
