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
import com.pame.karsy.data.repository.UserRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Formulario de registro (particular y lote).
 * Solo se exige llenar los campos que la BD necesita; la contraseña no tiene reglas,
 * solo debe coincidir con la confirmación.
 */
class RegisterViewModel : ViewModel() {
    // Datos personales / del responsable
    var nombre by mutableStateOf("")
    var apellido by mutableStateOf("")
    var correo by mutableStateOf("")
    var telefono by mutableStateOf("")
    var ciudad by mutableStateOf("")
    var estado by mutableStateOf("")

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

    fun errorFor(field: String): String? = if (field in missing) "Campo obligatorio" else null

    val passwordMismatch: String?
        get() = if (confirmPassword.isNotEmpty() && password != confirmPassword) "Las contraseñas no coinciden." else null

    fun submit(esLote: Boolean, context: Context, onCreated: (SessionAccount) -> Unit) {
        if (loading) return
        val requeridos = buildMap {
            put("nombre", nombre); put("apellido", apellido); put("correo", correo)
            put("ciudad", ciudad); put("estado", estado); put("password", password)
            if (esLote) {
                put("nombreLote", nombreLote); put("calle", calle); put("numero", numero)
                put("colonia", colonia); put("codigoPostal", codigoPostal)
            }
        }
        missing = requeridos.filterValues { it.isBlank() }.keys
        error = when {
            missing.isNotEmpty() -> "Completa los campos obligatorios."
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
            put("telefono", telefono.trim())
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
                .onFailure {
                    error = it.mensajeUsuario()
                    loading = false
                }
        }
    }
}
