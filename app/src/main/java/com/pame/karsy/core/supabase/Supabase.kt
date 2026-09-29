package com.pame.karsy.core.supabase

import android.util.Log
import com.pame.karsy.BuildConfig
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.util.UserFacingException
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import java.io.IOException

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

private val ERROR_GENERICO: String get() = texto(R.string.core_error_generic)

/**
 * Mensaje (en el idioma de la app) para mostrar al usuario. Nunca muestra textos técnicos
 * (de Supabase, de la BD o de librerías): esos se registran en Logcat.
 */
fun Throwable.mensajeUsuario(): String = when (this) {
    is UserFacingException -> message ?: ERROR_GENERICO
    is AuthRestException -> when (error) {
        "invalid_credentials", "invalid_grant" -> texto(R.string.core_error_invalid_credentials)
        "user_already_exists", "email_exists" -> texto(R.string.core_error_email_exists)
        "weak_password" -> texto(R.string.core_error_weak_password)
        "email_not_confirmed" -> texto(R.string.core_error_email_not_confirmed)
        "otp_expired" -> texto(R.string.core_error_otp_expired)
        "same_password" -> texto(R.string.core_error_same_password)
        "over_email_send_rate_limit", "over_request_rate_limit" ->
            texto(R.string.core_error_rate_limit)
        "validation_failed", "email_address_invalid" -> texto(R.string.core_error_invalid_email)
        "email_address_not_authorized" -> texto(R.string.core_error_email_not_authorized)
        else -> generico()
    }
    // P0001 = RAISE EXCEPTION de las funciones de la BD, que ya vienen en español.
    is PostgrestRestException -> when (code) {
        "P0001" -> error
        "42501" -> texto(R.string.core_error_no_permission)
        else -> generico()
    }
    is HttpRequestException, is IOException -> texto(R.string.core_error_no_connection)
    else -> generico()
}

private fun Throwable.generico(): String {
    Log.w("Karsy", "Error sin mensaje para el usuario", this)
    return ERROR_GENERICO
}
