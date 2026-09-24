package com.pame.karsy.feature.publish

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar

/**
 * Estado del flujo "Publicar vehículo" (4 pasos + overlay de éxito).
 * Los datos del formulario viven aquí para no perderse al ir y volver entre pasos.
 * Al publicar se suben las fotos a Storage y se llama a crear_publicacion(), que deja
 * la publicación en revisión (propuesta pendiente) hasta que un admin la apruebe.
 */
class PublishViewModel : ViewModel() {

    var step by mutableIntStateOf(1)
        private set
    var published by mutableStateOf(false)
        private set
    var publishing by mutableStateOf(false)
        private set
    /** Mensaje de validación o error del paso actual. */
    var error by mutableStateOf<String?>(null)
        private set

    var catalogs by mutableStateOf<Catalogs?>(null)
        private set

    // Paso 1: Datos (ids de catálogo de la BD)
    var idMarca by mutableStateOf<Long?>(null)
        private set
    var marcaOtra by mutableStateOf("")
    var idModelo by mutableStateOf<Long?>(null)
    var modeloOtro by mutableStateOf("")
    var anio by mutableStateOf("")
    var precio by mutableStateOf("")
    var idTransmision by mutableStateOf<Long?>(null)
    var kilometraje by mutableStateOf("")
    var cilindros by mutableStateOf("")
    var caballos by mutableStateOf("")
    var idCarroceria by mutableStateOf<Long?>(null)
    var idColor by mutableStateOf<Long?>(null)
    var duenos by mutableStateOf("")

    // Paso 2: una foto por ángulo (frontal, trasera, izquierda, derecha, tablero)
    val photos = mutableStateListOf<Uri?>(null, null, null, null, null)

    // Paso 3: Detalles
    var descripcion by mutableStateOf("")
    var imperfecciones by mutableStateOf("")

    init {
        viewModelScope.launch {
            safeCall { CatalogRepository.get() }
                .onSuccess { catalogs = it }
                .onFailure { error = it.mensajeUsuario() }
        }
    }

    // ── Nombres para mostrar ─────────────────────────────────────────────────

    val marcaEsOtro: Boolean get() = catalogs?.marcas?.firstOrNull { it.id == idMarca }?.esOtro == true
    val modeloEsOtro: Boolean get() = catalogs?.modelos?.firstOrNull { it.id == idModelo }?.esOtro == true

    val marcaNombre: String
        get() = if (marcaEsOtro) marcaOtra.trim()
        else catalogs?.marcas?.firstOrNull { it.id == idMarca }?.nombre.orEmpty()
    val modeloNombre: String
        get() = if (modeloEsOtro) modeloOtro.trim()
        else catalogs?.modelos?.firstOrNull { it.id == idModelo }?.nombre.orEmpty()
    val transmisionNombre: String
        get() = catalogs?.transmisiones?.firstOrNull { it.id == idTransmision }?.nombre.orEmpty()
    val carroceriaNombre: String
        get() = catalogs?.carrocerias?.firstOrNull { it.id == idCarroceria }?.nombre.orEmpty()
    val colorNombre: String
        get() = catalogs?.colores?.firstOrNull { it.id == idColor }?.nombre.orEmpty()

    val titulo: String get() = listOf(marcaNombre, modeloNombre, anio.trim()).filter { it.isNotEmpty() }.joinToString(" ")
    val precioTexto: String get() = Formato.precio(precio.filter(Char::isDigit).toDoubleOrNull())
    val fotosElegidas: List<Uri> get() = photos.filterNotNull()

    fun selectMarca(id: Long) {
        if (id != idMarca) {
            idMarca = id
            idModelo = null
            modeloOtro = ""
        }
    }

    // ── Navegación entre pasos ───────────────────────────────────────────────

    /** Valida el paso actual y avanza. */
    fun next() {
        error = validate(step)
        if (error == null && step < 4) step++
    }

    /** Regresa un paso. Devuelve false si ya estamos en el paso 1 (hay que salir del flujo). */
    fun back(): Boolean {
        error = null
        if (step == 1) return false
        step--
        return true
    }

    private fun validate(paso: Int): String? = when (paso) {
        1 -> {
            val anioActual = Calendar.getInstance().get(Calendar.YEAR)
            val anioNum = anio.trim().toIntOrNull()
            when {
                idMarca == null || (marcaEsOtro && marcaOtra.isBlank()) -> "Elige la marca."
                idModelo == null || (modeloEsOtro && modeloOtro.isBlank()) -> "Elige el modelo."
                anioNum == null || anioNum !in 1900..(anioActual + 1) -> "Escribe un año válido."
                (precio.filter(Char::isDigit).toLongOrNull() ?: 0) <= 0 -> "Escribe el precio."
                idTransmision == null -> "Elige la transmisión."
                kilometraje.filter(Char::isDigit).isEmpty() -> "Escribe el kilometraje."
                idCarroceria == null -> "Elige el tipo de carro."
                idColor == null -> "Elige el color."
                else -> null
            }
        }
        2 -> if (fotosElegidas.isEmpty()) "Agrega al menos una foto." else null
        3 -> if (descripcion.isBlank()) "Escribe una descripción del vehículo." else null
        else -> null
    }

    // ── Publicar ─────────────────────────────────────────────────────────────

    fun publish(context: Context) {
        if (publishing) return
        error = (1..3).firstNotNullOfOrNull { validate(it) }
        if (error != null) return

        val appContext = context.applicationContext
        publishing = true
        viewModelScope.launch {
            safeCall {
                val rutas = fotosElegidas.map { uri ->
                    CarRepository.uploadPhoto(Imagenes.jpegBytes(appContext, uri))
                }
                // "Caballos de fuerza" no tiene columna en la BD: se agrega a la descripción.
                val potencia = caballos.filter(Char::isDigit)
                val descripcionFinal = descripcion.trim() +
                    if (potencia.isNotEmpty()) "\n\nPotencia: $potencia hp" else ""

                val datos = buildJsonObject {
                    put("titulo", titulo)
                    put("descripcion", descripcionFinal)
                    put("precio", precio.filter(Char::isDigit).toLong())
                    put("moneda", "MXN")
                    put("id_marca", idMarca)
                    put("marca_otra", if (marcaEsOtro) marcaOtra.trim() else null)
                    put("id_modelo", idModelo)
                    put("modelo_otro", if (modeloEsOtro) modeloOtro.trim() else null)
                    put("anio", anio.trim().toInt())
                    put("kilometraje", kilometraje.filter(Char::isDigit).toLong())
                    put("id_color", idColor)
                    put("id_transmision", idTransmision)
                    put("id_carroceria", idCarroceria)
                    put("numero_propietarios_anteriores", duenos.filter(Char::isDigit).toIntOrNull() ?: 0)
                    put("cilindros", cilindros.filter(Char::isDigit).toIntOrNull())
                    put("descripcion_problemas", imperfecciones.trim())
                }
                CarRepository.publish(datos, rutas)
            }
                .onSuccess { published = true }
                .onFailure { error = it.mensajeUsuario() }
            publishing = false
        }
    }
}
