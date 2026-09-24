package com.pame.karsy.core.supabase

import com.pame.karsy.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json

/**
 * Cliente único de Supabase (Auth + base de datos + Storage).
 * La URL y la anon key se leen de local.properties (ver app/build.gradle.kts).
 */
object Supabase {

    /** Bucket público donde se guardan fotos de publicaciones y perfiles. */
    const val BUCKET = "karsy"

    val client: SupabaseClient by lazy {
        check(BuildConfig.SUPABASE_ANON_KEY.isNotBlank()) {
            "Falta SUPABASE_ANON_KEY en local.properties."
        }
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            defaultSerializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
            install(Auth)
            install(Postgrest)
            install(Storage)
        }
    }

    /**
     * Convierte una ruta guardada en la BD en una URL que Coil puede cargar.
     * Los datos de prueba guardan URLs completas; las fotos subidas desde la app
     * guardan la ruta dentro del bucket.
     */
    fun publicUrl(ruta: String?): String? = when {
        ruta.isNullOrBlank() -> null
        ruta.startsWith("http") -> ruta
        else -> "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET/$ruta"
    }
}

/** Mensaje en español para mostrar al usuario a partir de un error de Supabase. */
fun Throwable.mensajeUsuario(): String = when (this) {
    is AuthRestException -> when (error) {
        "invalid_credentials", "invalid_grant" -> "Correo o contraseña incorrectos."
        "user_already_exists", "email_exists" -> "Ya existe una cuenta con ese correo."
        "weak_password" -> "Supabase pide al menos 6 caracteres en la contraseña."
        "email_not_confirmed" -> "Confirma tu correo antes de iniciar sesión."
        "over_email_send_rate_limit", "over_request_rate_limit" ->
            "Demasiados intentos. Espera unos minutos e inténtalo de nuevo."
        "validation_failed", "email_address_invalid" -> "Revisa que el correo sea válido."
        else -> errorDescription
    }
    // Errores de la BD: los RAISE EXCEPTION de las funciones ya vienen en español.
    is RestException -> error
    is HttpRequestException -> "Sin conexión con el servidor. Revisa tu Internet."
    is IllegalStateException, is IllegalArgumentException -> message ?: "Ocurrió un error."
    else -> message ?: "Ocurrió un error inesperado."
}
