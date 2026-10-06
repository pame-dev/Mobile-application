package com.pame.karsy.core.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import androidx.core.util.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/** Municipio y estado donde está el teléfono (para "Cerca de mí"). */
data class Lugar(val municipio: String, val estado: String)

/**
 * Ubicación aproximada del teléfono convertida a municipio y estado. Las publicaciones
 * guardan estado y municipio (no coordenadas), así que se compara por nombre.
 * Requiere el permiso ACCESS_COARSE_LOCATION (lo pide la pantalla antes de llamar).
 */
object UbicacionActual {

    /** null si no hay ubicación disponible o no se pudo convertir a un lugar. */
    suspend fun obtener(context: Context): Lugar? {
        val location = posicion(context) ?: return null
        val address = direccion(context, location) ?: return null
        val estado = address.adminArea ?: return null
        val municipio = address.locality ?: address.subAdminArea ?: ""
        return Lugar(municipio, estado)
    }

    /** Compara nombres sin mayúsculas ni acentos: "Coahuila" = "Coahuila de Zaragoza". */
    fun mismoLugar(a: String, b: String): Boolean {
        val x = normalizar(a)
        val y = normalizar(b)
        if (x.isEmpty() || y.isEmpty()) return false
        return x == y || x.startsWith(y) || y.startsWith(x)
    }

    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .replace("estado de ", "")
            .trim()

    @SuppressLint("MissingPermission")
    private suspend fun posicion(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        // Primero la ubicación combinada de Android (12+), luego red y GPS.
        val candidatos = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        val proveedor = candidatos
            .firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) } ?: return null
        // Primero una lectura nueva; si tarda, la última conocida.
        val actual = withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine<Location?> { cont ->
                LocationManagerCompat.getCurrentLocation(
                    manager, proveedor, null as CancellationSignal?, Executors.newSingleThreadExecutor(),
                    Consumer<Location?> { cont.resume(it) }
                )
            }
        }
        return actual ?: runCatching { manager.getLastKnownLocation(proveedor) }.getOrNull()
    }

    private suspend fun direccion(context: Context, location: Location): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("es", "MX"))
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            withTimeoutOrNull(8_000) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(location.latitude, location.longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) { cont.resume(addresses.firstOrNull()) }
                        override fun onError(errorMessage: String?) { cont.resume(null) }
                    })
                }
            }
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull() }.getOrNull()
        }
    }
}
