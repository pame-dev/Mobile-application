package com.pame.karsy.core.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Idioma de la app (español o inglés), elegido por el usuario y guardado en preferencias.
 * Si no ha elegido, o eligió [SISTEMA], se sigue el idioma del teléfono: inglés si el
 * teléfono está en inglés; si no, español.
 * Los textos viven en res/values (español) y res/values-en (inglés).
 */
object Idioma {
    const val ES = "es"
    const val EN = "en"
    /** Seguir el idioma del teléfono. */
    const val SISTEMA = "system"

    private const val PREFS = "karsy_prefs"
    private const val KEY = "idioma"

    /** Contexto con el idioma elegido, para textos que se arman fuera de Compose (ViewModels, repositorios). */
    private lateinit var contexto: Context

    /** Idioma en uso: [ES] o [EN]. */
    fun actual(context: Context): String = when (val elegido = elegido(context)) {
        ES, EN -> elegido
        else -> delTelefono()
    }

    /** Lo que eligió el usuario: [ES], [EN] o [SISTEMA] (también si nunca eligió). */
    fun elegido(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: SISTEMA

    /**
     * Idioma del teléfono. Se lee de la configuración del sistema porque [envolver]
     * cambia Locale.getDefault() al idioma de la app.
     */
    fun delTelefono(): String =
        if (Resources.getSystem().configuration.locales[0].language == EN) EN else ES

    /** Aplica el idioma guardado al contexto de la Activity (se llama en attachBaseContext). */
    fun envolver(base: Context): Context {
        val locale = Locale.forLanguageTag(actual(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        contexto = base.applicationContext.createConfigurationContext(config)
        return base.createConfigurationContext(config)
    }

    internal fun contextoActual(): Context = contexto

    /**
     * Guarda el idioma ([ES], [EN] o [SISTEMA]) y recrea la Activity para que toda la UI
     * lo tome. Si el idioma en uso no cambia (p. ej. de "Español" a "teléfono en español"),
     * solo se guarda.
     */
    fun cambiar(activity: Activity, codigo: String) {
        if (codigo == elegido(activity)) return
        val antes = actual(activity)
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, codigo).apply()
        if (actual(activity) != antes) activity.recreate()
    }
}

/** Texto traducido para usar fuera de Compose (en Compose usa stringResource). */
fun texto(@StringRes id: Int, vararg args: Any): String = Idioma.contextoActual().getString(id, *args)

/** Plural traducido para usar fuera de Compose (en Compose usa pluralStringResource). */
fun textoPlural(@PluralsRes id: Int, cantidad: Int, vararg args: Any): String =
    Idioma.contextoActual().resources.getQuantityString(id, cantidad, *args)
