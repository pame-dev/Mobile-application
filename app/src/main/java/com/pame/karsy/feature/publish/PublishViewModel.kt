package com.pame.karsy.feature.publish

import android.content.Context
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.util.TapaPlacas
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.Job
import com.pame.karsy.core.util.DetectorAutos
import androidx.compose.runtime.mutableStateMapOf
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
    /** Foto del motor: recomendada, no obligatoria. Se sube después de los 5 ángulos. */
    var enginePhoto by mutableStateOf<Uri?>(null)
        private set
    /** Fotos del tablero en las que el detector ve el exterior de un auto (solo se avisa). */
    private val tableroExterior = mutableStateMapOf<Uri, Boolean>()
    /** Fotos que ya estaban en Storage (al corregir): no se vuelven a subir. */
    private val rutasExistentes = mutableMapOf<Uri, String>()

    /** Ya se intentó pasar del paso 2: se marcan en rojo los ángulos sin foto. */
    var photosAttempted by mutableStateOf(false)
        private set
    /** Fotos idénticas a otra (mismo contenido), marcadas en rojo. */
    var duplicatePhotos by mutableStateOf<Set<Uri>>(emptySet())
        private set
    /**
     * ¿Parece auto? por foto de ángulo (frontal, trasera, laterales). Sin entrada = se está
     * revisando. Las que ya estaban en Storage (al editar) ya fueron aprobadas y no se revisan.
     */
    val revisionAuto = mutableStateMapOf<Uri, Boolean>()

    /** Fotos nuevas a las que se les están tapando las placas (TapaPlacas). */
    private val procesando = mutableStateMapOf<Uri, Boolean>()
    private val trabajosFotos = mutableListOf<Job>()

    fun procesandoFoto(uri: Uri): Boolean = uri in procesando
    fun revisandoAuto(uri: Uri): Boolean = uri in procesando || (uri !in rutasExistentes && uri !in revisionAuto)
    fun noPareceAuto(uri: Uri): Boolean = revisionAuto[uri] == false
    fun pareceExterior(uri: Uri): Boolean = tableroExterior[uri] == true

    /**
     * Pone la foto de un ángulo. Al momento se le tapan las placas (menos al tablero, que no
     * tiene y donde se ve el kilometraje) y las de los primeros 4 se revisan como auto.
     */
    fun setAnglePhoto(context: Context, slot: Int, uri: Uri) {
        photos[slot]?.let { anterior -> tableroExterior.remove(anterior) }
        photos[slot] = uri
        val esTablero = slot == photos.lastIndex
        prepararFoto(context, uri, taparPlacas = !esTablero) { lista ->
            if (photos[slot] == uri) photos[slot] = lista // si no la cambiaron mientras tanto
            if (slot < DetectorAutos.ANGULOS_CON_AUTO) revisarAuto(context.applicationContext, lista)
        }
        // En el tablero se avisa si se ve el auto por fuera (sin bloquear: el detector puede
        // fallar). Va aparte para que "Siguiente" no tenga que esperarla.
        if (esTablero) {
            val appContext = context.applicationContext
            viewModelScope.launch {
                if (DetectorAutos.pareceAuto(appContext, uri) == true) tableroExterior[uri] = true
            }
        }
    }

    /** Ya no cabe la foto del motor: entre ángulos y adicionales se llegó a [MAX_FOTOS]. */
    val motorSinLugar: Boolean get() = enginePhoto == null && fotosElegidas.size >= MAX_FOTOS

    /** Pone la foto del motor (recomendada); se le tapan las placas como a las demás. */
    fun setEnginePhoto(context: Context, uri: Uri) {
        if (motorSinLugar) return
        enginePhoto?.let { duplicatePhotos = duplicatePhotos - it }
        enginePhoto = uri
        prepararFoto(context, uri, taparPlacas = true) { lista ->
            if (enginePhoto == uri) enginePhoto = lista
        }
    }

    fun removeEnginePhoto() {
        enginePhoto?.let { duplicatePhotos = duplicatePhotos - it }
        enginePhoto = null
    }

    /**
     * Tapa las placas de una foto y llama a [listo] con la versión que se va a subir. Con
     * [soloSiHayPlaca] (fotos ya publicadas) devuelve la misma si no tenía placa.
     */
    private fun prepararFoto(
        context: Context,
        uri: Uri,
        taparPlacas: Boolean,
        soloSiHayPlaca: Boolean = false,
        listo: suspend (Uri) -> Unit,
    ) {
        val appContext = context.applicationContext
        procesando[uri] = true
        trabajosFotos += viewModelScope.launch {
            val lista = if (taparPlacas) TapaPlacas.tapar(appContext, uri, soloSiHayPlaca) else uri
            listo(lista)
            procesando.remove(uri)
        }
    }

    private var existentesRevisadas = false

    /**
     * Al editar: revisa también las fotos que ya estaban publicadas (menos el tablero). Si
     * alguna tiene placa, se reemplaza por la versión tapada y esa es la que se sube; así,
     * aunque el dueño no cambie las fotos, el admin las ve censuradas al revisar la edición.
     */
    fun taparPlacasExistentes(context: Context) {
        if (!isEditing || loadingEdit || existentesRevisadas) return
        existentesRevisadas = true
        val appContext = context.applicationContext
        photos.toList().forEachIndexed { slot, uri ->
            if (uri == null || uri !in rutasExistentes || slot == photos.lastIndex) return@forEachIndexed
            prepararFoto(appContext, uri, taparPlacas = true, soloSiHayPlaca = true) { lista ->
                if (lista != uri && photos[slot] == uri) {
                    photos[slot] = lista
                    if (slot < DetectorAutos.ANGULOS_CON_AUTO) revisarAuto(appContext, lista)
                }
            }
        }
        extraPhotos.toList().filter { it in rutasExistentes }.forEach { uri ->
            prepararFoto(appContext, uri, taparPlacas = true, soloSiHayPlaca = true) { lista ->
                val i = extraPhotos.indexOf(uri)
                if (lista != uri && i >= 0) extraPhotos[i] = lista
            }
        }
    }

    private suspend fun revisarAuto(context: Context, uri: Uri) {
        if (!revisandoAuto(uri)) return
        // Si ML Kit falla, no se bloquea al usuario (la publicación igual la revisa un admin).
        revisionAuto[uri] = DetectorAutos.pareceAuto(context, uri) ?: true
    }

    /** Al publicar: (fotos subidas, total) mientras se suben a Storage; null el resto del tiempo. */
    var uploadProgress by mutableStateOf<Pair<Int, Int>?>(null)
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
    val fotosElegidas: List<Uri> get() = photos.filterNotNull() + listOfNotNull(enginePhoto) + extraPhotos
    /** Cuántas fotos adicionales caben todavía. */
    val extraPhotosLeft: Int get() = MAX_FOTOS - fotosElegidas.size

    fun addExtraPhotos(context: Context, uris: List<Uri>) {
        val nuevas = uris.filter { it !in extraPhotos }.take(extraPhotosLeft)
        extraPhotos += nuevas
        nuevas.forEach { uri ->
            prepararFoto(context, uri, taparPlacas = true) { lista ->
                val i = extraPhotos.indexOf(uri)
                if (i >= 0) extraPhotos[i] = lista
            }
        }
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

    /** "foto del tablero, foto frontal": nombres de los ángulos en minúscula para los avisos. */
    private fun nombresFotos(slots: List<Int>): String =
        slots.joinToString(", ") { texto(ETIQUETAS_ANGULOS[it]).replaceFirstChar { c -> c.lowercase() } }

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
     * Paso 2 → 3: deben estar las 5 fotos obligatorias, las 4 de ángulos deben parecer
     * de un auto (DetectorAutos) y ninguna puede repetirse.
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
            // Primero terminan de taparse las placas de las fotos nuevas.
            trabajosFotos.toList().joinAll()
            trabajosFotos.clear()
            // Las fotos de los ángulos deben mostrar el auto (espera las que se estén revisando).
            val angulos = photos.take(DetectorAutos.ANGULOS_CON_AUTO).filterNotNull()
            angulos.forEach { revisarAuto(appContext, it) }
            val noSonAuto = photos.take(DetectorAutos.ANGULOS_CON_AUTO).indices.filter { i -> photos[i]?.let(::noPareceAuto) == true }
            if (noSonAuto.isNotEmpty()) {
                checkingPhotos = false
                error = texto(R.string.publish_error_not_car, nombresFotos(noSonAuto))
                photoErrorSignal++
                return@launch
            }
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
        2 -> photos.indices.filter { photos[it] == null }
            .takeIf { it.isNotEmpty() }
            ?.let { texto(R.string.publish_error_required_photos, nombresFotos(it)) }
            ?: if (fotosElegidas.size > MAX_FOTOS) texto(R.string.publish_photos_limit, MAX_FOTOS) else null
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
            // Por si alguna foto todavía se está procesando (placas).
            trabajosFotos.toList().joinAll()
            safeCall {
                // editar_publicacion solo acepta fotos de la carpeta del dueño. Las ya publicadas
                // que están en otra (datos de prueba, links externos) se suben de nuevo a la suya.
                val propia = "publicaciones/${SessionManager.userId}/"
                val fotos = fotosElegidas
                val porSubir = fotos.count { rutasExistentes[it]?.startsWith(propia) != true }
                var subidas = 0
                if (porSubir > 0) uploadProgress = 0 to porSubir
                val rutas = fotos.map { uri ->
                    rutasExistentes[uri]?.takeIf { it.startsWith(propia) }
                        ?: CarRepository.uploadPhoto(Imagenes.jpegBytes(appContext, uri)).also {
                            subidas++
                            uploadProgress = subidas to porSubir
                        }
                }
                // Fotos listas: lo que sigue es guardar la publicación ("Publicando…").
                uploadProgress = null
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
            uploadProgress = null
            publishing = false
        }
    }

    companion object {
        /** Nombre de cada ángulo obligatorio, en el orden de [photos]. */
        val ETIQUETAS_ANGULOS = listOf(
            R.string.publish_photo_front,
            R.string.publish_photo_rear,
            R.string.publish_photo_left,
            R.string.publish_photo_right,
            R.string.publish_photo_dashboard,
        )

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
