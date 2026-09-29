package com.pame.karsy.core.locale

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import java.util.Locale

/**
 * Idioma de la app (español o inglés), elegido por el usuario y guardado en preferencias.
 * La primera vez se usa el idioma del teléfono si es inglés; si no, español.
 * Los textos viven en res/values (español) y res/values-en (inglés).
 */
object Idioma {
    const val ES = "es"
    const val EN = "en"

    private const val PREFS = "karsy_prefs"
    private const val KEY = "idioma"

    /** Contexto con el idioma elegido, para textos que se arman fuera de Compose (ViewModels, repositorios). */
    private lateinit var contexto: Context

    fun actual(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
            ?: if (Locale.getDefault().language == EN) EN else ES

    /** Aplica el idioma guardado al contexto de la Activity (se llama en attachBaseContext). */
    fun envolver(base: Context): Context {
        val locale = Locale.forLanguageTag(actual(base))
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        contexto = base.applicationContext.createConfigurationContext(config)
        return base.createConfigurationContext(config)
    }

    internal fun contextoActual(): Context = contexto

    /** Guarda el idioma y recrea la Activity para que toda la UI lo tome. */
    fun cambiar(activity: Activity, codigo: String) {
        if (codigo == actual(activity)) return
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, codigo).apply()
        activity.recreate()
    }
}

/** Texto traducido para usar fuera de Compose (en Compose usa stringResource). */
fun texto(@StringRes id: Int, vararg args: Any): String = Idioma.contextoActual().getString(id, *args)

/** Plural traducido para usar fuera de Compose (en Compose usa pluralStringResource). */
fun textoPlural(@PluralsRes id: Int, cantidad: Int, vararg args: Any): String =
    Idioma.contextoActual().resources.getQuantityString(id, cantidad, *args)
