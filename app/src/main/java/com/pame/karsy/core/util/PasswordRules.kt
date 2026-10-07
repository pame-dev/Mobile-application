package com.pame.karsy.core.util

import androidx.annotation.StringRes
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto

/**
 * Reglas de contraseña de la app (registro particular y lote, recuperación y cambio).
 * Supabase también debe exigir lo mismo (minimum_password_length y password_requirements).
 */
object PasswordRules {
    const val MIN_LENGTH = 8
    /** Supabase (bcrypt) solo toma en cuenta los primeros 72 bytes. */
    const val MAX_LENGTH = 72

    /** Requisitos que se muestran como lista en el formulario. */
    enum class Rule(@StringRes val label: Int, val cumple: (String) -> Boolean) {
        LENGTH(R.string.password_rule_length, { it.length >= MIN_LENGTH }),
        UPPER(R.string.password_rule_upper, { p -> p.any { it.isUpperCase() } }),
        LOWER(R.string.password_rule_lower, { p -> p.any { it.isLowerCase() } }),
        DIGIT(R.string.password_rule_digit, { p -> p.any { it.isDigit() } }),
        SYMBOL(R.string.password_rule_symbol, { p -> p.any { !it.isLetterOrDigit() && !it.isWhitespace() } }),
        NO_SPACES(R.string.password_rule_no_spaces, { p -> p.isNotEmpty() && p.none { it.isWhitespace() } }),
    }

    fun faltantes(password: String): List<Rule> = Rule.entries.filterNot { it.cumple(password) }

    fun esValida(password: String, correo: String? = null): Boolean = error(password, correo) == null

    /** Primer problema de la contraseña, o null si es válida. [correo] evita que la contenga. */
    fun error(password: String, correo: String? = null): String? {
        val usuario = correo?.substringBefore('@')?.trim().orEmpty()
        return when {
            password.length > MAX_LENGTH -> texto(R.string.password_error_too_long, MAX_LENGTH)
            faltantes(password).isNotEmpty() -> texto(R.string.password_error_requirements)
            usuario.length >= 3 && password.contains(usuario, ignoreCase = true) ->
                texto(R.string.password_error_contains_email)
            else -> null
        }
    }
}
