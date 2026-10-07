package com.pame.karsy.feature.publish

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.navigation.Routes
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.repository.CarRepository
import com.pame.karsy.data.repository.CatalogRepository
import com.pame.karsy.data.repository.Catalogs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar

/**
 * Estado del flujo "Publicar vehículo" (4 pasos + overlay de éxito).
 * Los datos del formulario viven aquí para no perderse al ir y volver entre pasos.
 * Al publicar se suben las fotos a Storage y se llama a crear_publicacion(), que deja
 * la publicación en revisión (propuesta pendiente) hasta que un admin la apruebe.
 *
 * Con el argumento editId el formulario se precarga con una publicación propia
 * (aprobada o rechazada) y al terminar se llama a editar_publicacion(), que manda
 * los cambios a revisión.
 */
class PublishViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    /** Publicación propia que se está editando; null al publicar una nueva. */
    val editId: Long? = savedStateHandle.get<Long>(Routes.PublishFlow.ARG_EDIT)?.takeIf { it > 0 }
    val isEditing: Boolean get() = editId != null

    /** Mientras se cargan los datos de la publicación a editar. */
    var loadingEdit by mutableStateOf(editId != null)
        private set
    /** Motivo con el que administración rechazó la publicación que se corrige. */
    var rejectedReason by mutableStateOf<String?>(null)
        private set
    /** La publicación editada ya estaba aprobada: sigue visible mientras se revisan los cambios. */
    var editingApproved by mutableStateOf(false)
        private set

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
    /** Columna recuperado_por_seguro. */
    var recuperadoPorSeguro by mutableStateOf(false)
    var cilindros by mutableStateOf("")
    var caballos by mutableStateOf("")
    /** Cilindrada / tipo de motor en texto libre (columna cilindrada), p. ej. "2.0 L Turbo". */
    var motor by mutableStateOf("")
    /** Uno de [COMBUSTIBLES] o vacío (columna tipo_combustible). */
    var combustible by mutableStateOf("")
    var idCarroceria by mutableStateOf<Long?>(null)
    var idColor by mutableStateOf<Long?>(null)
    var duenos by mutableStateOf("")

    // Paso 2: una foto por ángulo (frontal, trasera, izquierda, derecha, tablero)
    // y hasta completar MAX_FOTOS con fotos adicionales.
    val photos = mutableStateListOf<Uri?>(null, null, null, null, null)
    val extraPhotos = mutableStateListOf<Uri>()
    /** Fotos que ya estaban en Storage (al corregir): no se vuelven a subir. */
    private val rutasExistentes = mutableMapOf<Uri, String>()

    /** Ya se intentó pasar del paso 2: se marcan en rojo los ángulos sin foto. */
    var photosAttempted by mutableStateOf(false)
        private set
    /** Fotos idénticas a otra (mismo contenido), marcadas en rojo. */
    var duplicatePhotos by mutableStateOf<Set<Uri>>(emptySet())
        private set
    /** Mientras se comparan las fotos para buscar repetidas. */
    var checkingPhotos by mutableStateOf(false)
        private set
    /** Cambia cada vez que falla la validación de fotos: la pantalla sube para mostrar el aviso. */
    var photoErrorSignal by mutableIntStateOf(0)
        private set
    /** Huella (SHA-256 del contenido) de cada foto ya revisada, para no volver a leerla. */
    private val huellas = mutableMapOf<Uri, String>()

    // Paso 3: Detalles
    var descripcion by mutableStateOf("")
    var imperfecciones by mutableStateOf("")

    init {
        viewModelScope.launch {
            safeCall { CatalogRepository.get() }
                .onSuccess { catalogs = it }
                .onFailure { error = it.mensajeUsuario() }
            // Los catálogos van primero: se usan para saber si la marca/modelo es "Otros".
            editId?.let { id ->
                safeCall { CarRepository.ownListing(id) }
                    .onSuccess { a -> if (a != null) prefill(a) else error = texto(R.string.publish_edit_not_found) }
                    .onFailure { error = it.mensajeUsuario() }
                loadingEdit = false
            }
        }
    }

    /** Llena el formulario con los datos que tenía la publicación. */
    private fun prefill(a: AnuncioDto) {
        rejectedReason = a.motivoRechazo?.takeIf { a.estadoUltimaPropuesta == "rechazada" }
        editingApproved = a.aprobada
        idMarca = a.idMarca
        idModelo = a.idModelo
        if (marcaEsOtro) marcaOtra = a.marca.orEmpty()
        if (modeloEsOtro) modeloOtro = a.modelo.orEmpty()
        anio = a.anio?.toString().orEmpty()
        precio = a.precio?.toLong()?.toString().orEmpty()
        idTransmision = a.idTransmision
        kilometraje = a.kilometraje?.toString().orEmpty()
        recuperadoPorSeguro = a.recuperadoPorSeguro == true
        cilindros = a.cilindros?.toString().orEmpty()
        motor = a.cilindrada.orEmpty()
        combustible = a.combustible.orEmpty()
        idCarroceria = a.idCarroceria
        idColor = a.idColor
        duenos = a.propietariosAnteriores?.toString().orEmpty()

        // Al publicar, los caballos de fuerza se agregaron al final de la descripción.
        val desc = a.descripcion.orEmpty()
        val potencia = POTENCIA.find(desc)
        caballos = potencia?.groupValues?.get(1).orEmpty()
        descripcion = potencia?.let { desc.removeRange(it.range) } ?: desc
        imperfecciones = a.descripcionProblemas.orEmpty()

        // Las primeras van a los ángulos y el resto a fotos adicionales.
        a.fotos.take(MAX_FOTOS).forEachIndexed { i, ruta ->
            val uri = Supabase.publicUrl(ruta)?.toUri() ?: return@forEachIndexed
            rutasExistentes[uri] = ruta
            if (i < photos.size) photos[i] = uri else extraPhotos += uri
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
    val fotosElegidas: List<Uri> get() = photos.filterNotNull() + extraPhotos
    /** Cuántas fotos adicionales caben todavía. */
    val extraPhotosLeft: Int get() = MAX_FOTOS - fotosElegidas.size

    fun addExtraPhotos(uris: List<Uri>) {
        extraPhotos += uris.filter { it !in extraPhotos }.take(extraPhotosLeft)
    }

    fun removeExtraPhoto(uri: Uri) {
        extraPhotos.remove(uri)
    }

    fun selectMarca(id: Long) {
        if (id != idMarca) {
            idMarca = id
            idModelo = null
            modeloOtro = ""
        }
    }

    // ── Navegación entre pasos ───────────────────────────────────────────────

    /** Valida el paso actual y avanza. En el paso 2 además busca fotos repetidas. */
    fun next(context: Context) {
        if (step == 2) {
            nextFromPhotos(context)
            return
        }
        error = validate(step)
        if (error == null && step < 4) step++
    }

    /**
     * Paso 2 → 3: deben estar las 5 fotos obligatorias y ninguna puede repetirse.
     * Las repetidas se detectan por contenido, así que también cuenta la misma foto
     * elegida dos veces o dos copias idénticas con distinto nombre.
     */
    private fun nextFromPhotos(context: Context) {
        if (checkingPhotos) return
        photosAttempted = true
        error = validate(2)
        if (error != null) {
            photoErrorSignal++
            return
        }
        val appContext = context.applicationContext
        checkingPhotos = true
        viewModelScope.launch {
            val fotos = fotosElegidas
            val repetidas = fotos
                .groupBy { huella(appContext, it) }
                .values.filter { it.size > 1 }
                .flatten().toSet()
            duplicatePhotos = repetidas
            checkingPhotos = false
            if (repetidas.isNotEmpty()) {
                error = texto(R.string.publish_error_duplicate_photos)
                photoErrorSignal++
            } else {
                error = null
                step = 3
            }
        }
    }

    /**
     * SHA-256 del contenido de la foto. Las que ya estaban en Storage (al editar) se
     * identifican por su ruta, para no descargarlas.
     */
    private suspend fun huella(context: Context, uri: Uri): String = huellas.getOrPut(uri) {
        if (uri in rutasExistentes) return@getOrPut "ruta:" + rutasExistentes.getValue(uri)
        withContext(Dispatchers.IO) {
            runCatching {
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val leidos = input.read(buffer)
                        if (leidos < 0) break
                        digest.update(buffer, 0, leidos)
                    }
                } ?: return@runCatching null
                digest.digest().joinToString("") { "%02x".format(it) }
            }.getOrNull() ?: ("uri:$uri")
        }
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
                idMarca == null || (marcaEsOtro && marcaOtra.isBlank()) -> texto(R.string.publish_error_brand)
                idModelo == null || (modeloEsOtro && modeloOtro.isBlank()) -> texto(R.string.publish_error_model)
                anioNum == null || anioNum !in 1900..(anioActual + 1) -> texto(R.string.publish_error_year)
                (precio.filter(Char::isDigit).toLongOrNull() ?: 0) <= 0 -> texto(R.string.publish_error_price)
                idTransmision == null -> texto(R.string.publish_error_transmission)
                kilometraje.filter(Char::isDigit).isEmpty() -> texto(R.string.publish_error_mileage)
                (cilindros.filter(Char::isDigit).toIntOrNull() ?: 0) > MAX_CILINDROS -> texto(R.string.publish_error_cylinders, MAX_CILINDROS)
                idCarroceria == null -> texto(R.string.publish_error_body_type)
                idColor == null -> texto(R.string.publish_error_color)
                else -> null
            }
        }
        2 -> if (photos.any { it == null }) texto(R.string.publish_error_required_photos) else null
        3 -> if (descripcion.isBlank()) texto(R.string.publish_error_description) else null
        else -> null
    }

    // ── Publicar ─────────────────────────────────────────────────────────────

    fun publish(context: Context) {
        if (publishing || loadingEdit) return
        error = (1..3).firstNotNullOfOrNull { validate(it) }
        if (error != null) return

        val appContext = context.applicationContext
        publishing = true
        viewModelScope.launch {
            safeCall {
                val rutas = fotosElegidas.map { uri ->
                    rutasExistentes[uri] ?: CarRepository.uploadPhoto(Imagenes.jpegBytes(appContext, uri))
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
                    put("recuperado_por_seguro", recuperadoPorSeguro)
                    put("id_color", idColor)
                    put("id_transmision", idTransmision)
                    put("id_carroceria", idCarroceria)
                    put("numero_propietarios_anteriores", duenos.filter(Char::isDigit).toIntOrNull() ?: 0)
                    put("cilindros", cilindros.filter(Char::isDigit).toIntOrNull())
                    put("cilindrada", motor.trim().ifEmpty { null })
                    put("tipo_combustible", combustible.ifEmpty { null })
                    put("descripcion_problemas", imperfecciones.trim())
                }
                val id = editId
                if (id != null) CarRepository.edit(id, datos, rutas)
                else CarRepository.publish(datos, rutas)
            }
                .onSuccess { published = true }
                .onFailure { error = it.mensajeUsuario() }
            publishing = false
        }
    }

    companion object {
        /** Máximo de fotos por publicación (editar_publicacion valida lo mismo). */
        const val MAX_FOTOS = 15

        /** Máximo de publicaciones.cilindros (chk_publicaciones_cilindros). */
        const val MAX_CILINDROS = 16

        /** Opciones de combustible; se guardan tal cual en tipo_combustible (catálogo tipos_combustible). */
        val COMBUSTIBLES = listOf("Gasolina", "Diésel", "Híbrido", "Eléctrico")

        /** Largo máximo de publicaciones.cilindrada (VARCHAR(30)). */
        const val MOTOR_MAX = 30

        /** Sufijo "Potencia: N hp" que publish() agrega a la descripción. */
        private val POTENCIA = Regex("""\n\nPotencia: (\d+) hp$""")
    }
}
