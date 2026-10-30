package com.pame.karsy.feature.publish

import android.content.Context
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.util.HuellaVisual
import com.pame.karsy.core.util.PrecioSugerido
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
import com.pame.karsy.core.util.ArchivoConstancia
import com.pame.karsy.core.util.Constancia
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.Imagenes
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.DocumentoVehiculoDto
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

    /** Duplicar: publicación propia de la que se copian los datos (sin fotos ni documento). */
    val copyFrom: Long? = savedStateHandle.get<Long>(Routes.PublishFlow.ARG_COPY)?.takeIf { it > 0 && editId == null }

    /** Mientras se cargan los datos de la publicación a editar (o a duplicar). */
    var loadingEdit by mutableStateOf(editId != null || copyFrom != null)
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

    /** Campos que ya muestran su error: al salir de ellos o al tocar Siguiente. */
    var camposMarcados by mutableStateOf<Set<String>>(emptySet())
        private set
    /** Primer campo con error del último intento; la pantalla lo trae a la vista en cada [enfoqueSignal]. */
    var campoAEnfocar by mutableStateOf<String?>(null)
        private set
    var enfoqueSignal by mutableIntStateOf(0)
        private set

    fun marcarCampo(campo: String) {
        if (campo !in camposMarcados) camposMarcados = camposMarcados + campo
    }

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
    /** Columna factura_original. */
    var facturaOriginal by mutableStateOf(false)
    /** Columna aire_acondicionado. */
    var aireAcondicionado by mutableStateOf(false)
    var cilindros by mutableStateOf("")
    var caballos by mutableStateOf("")
    /** Litros del motor con un entero y un decimal ("2.0"); ver [litrosAlEscribir]. */
    var litros by mutableStateOf("")
    /** Uno de [ASPIRACIONES] o vacío. Junto con [litros] forma la columna cilindrada. */
    var aspiracion by mutableStateOf("")
    /** Texto que se guarda en cilindrada, p. ej. "2.0 L Turbo"; vacío si no hay datos. */
    val motor: String get() = if (esElectrico) "" else unirMotor(litros, aspiracion)

    /** Un eléctrico no lleva cilindros, litros ni aspiración. */
    val esElectrico: Boolean get() = combustible == ELECTRICO

    /** Elige el combustible; con eléctrico se borran los datos del motor de combustión. */
    fun selectCombustible(valor: String) {
        combustible = valor
        if (valor == ELECTRICO) {
            cilindros = ""
            litros = ""
            aspiracion = ""
        }
    }

    /**
     * Campos obligatorios del paso 1 en el orden del formulario: (campo, está lleno, error).
     * Cada campo se activa hasta que todos los anteriores están llenos.
     */
    private val ordenCampos: List<Triple<String, Boolean, Int>>
        get() = listOf(
            Triple("marca", idMarca != null && !(marcaEsOtro && marcaOtra.isBlank()), R.string.publish_error_brand),
            Triple("modelo", idModelo != null && !(modeloEsOtro && modeloOtro.isBlank()), R.string.publish_error_model),
            Triple("anio", anio.isNotBlank(), R.string.publish_error_year),
            Triple("transmision", idTransmision != null, R.string.publish_error_transmission),
            Triple("kilometraje", kilometraje.any(Char::isDigit), R.string.publish_error_mileage),
            Triple("carroceria", idCarroceria != null, R.string.publish_error_body_type),
            Triple("color", idColor != null, R.string.publish_error_color),
            Triple("duenos", duenos.isNotBlank(), R.string.publish_error_owners),
            Triple("condicion", condicion.isNotBlank(), R.string.publish_error_condition),
            Triple("combustible", combustible.isNotBlank(), R.string.publish_error_fuel),
            Triple("cilindros", esElectrico || cilindros.isNotBlank(), R.string.publish_error_cylinders_required),
            Triple("litros", esElectrico || (litros.toDoubleOrNull() ?: 0.0) > 0.0, R.string.publish_error_liters),
            Triple("aspiracion", esElectrico || aspiracion.isNotBlank(), R.string.publish_error_aspiration),
        )

    /** Falta llenar algún campo anterior a [campo]; los que no están en la lista van al final. */
    fun bloqueado(campo: String): Boolean {
        val lista = ordenCampos
        val i = lista.indexOfFirst { it.first == campo }.let { if (it < 0) lista.size else it }
        return lista.take(i).any { !it.second }
    }
    /** Uno de [COMBUSTIBLES] o vacío (columna tipo_combustible). */
    var combustible by mutableStateOf("")
    var idCarroceria by mutableStateOf<Long?>(null)
    var idColor by mutableStateOf<Long?>(null)
    var duenos by mutableStateOf("")
    /** Uno de [CONDICIONES] o vacío (columna condicion_vehiculo). */
    var condicion by mutableStateOf("")

    // Paso 1: documento del vehículo. Privado: solo lo ven el dueño y el admin.
    /** Uno de [CarRepository.TIPOS_DOCUMENTO] o vacío (columna tipo_documento_vehiculo). */
    var tipoDocumento by mutableStateOf("")
        private set
    /** VIN o placa tal como se escribe (sin espacios y en mayúsculas). */
    var documentoTexto by mutableStateOf("")
        private set
    /**
     * Foto (JPG o PNG) o PDF de la tarjeta de circulación o de la baja: el archivo local, o la URL
     * firmada de la foto ya guardada. Un PDF ya guardado no necesita URL: se muestra su nombre.
     */
    var documentoArchivo by mutableStateOf<Uri?>(null)
        private set
    /** El archivo es PDF: no tiene vista previa, se muestra [documentoNombre]. */
    var documentoEsPdf by mutableStateOf(false)
        private set
    var documentoNombre by mutableStateOf("")
        private set
    /** Por qué no sirve el archivo elegido (tipo o tamaño); se muestra junto al campo. */
    var documentoError by mutableStateOf<String?>(null)
        private set
    /** Archivo elegido en el teléfono, ya revisado (tipo y tamaño), que falta subir. */
    private var documentoNuevo: ArchivoConstancia? = null
    /** Ruta del archivo ya guardado en el bucket privado (al editar o tras subirlo). */
    private var documentoRuta: String? = null

    /** El documento elegido se da con archivo (tarjeta o baja), no escrito. */
    val documentoEsFoto: Boolean get() = tipoDocumento in TIPOS_DOCUMENTO_FOTO

    /** Cambiar de documento borra lo que se había puesto del anterior. */
    fun selectTipoDocumento(tipo: String) {
        if (tipo == tipoDocumento) return
        tipoDocumento = tipo
        documentoTexto = ""
        quitarArchivoDocumento()
    }

    fun escribirDocumento(texto: String) {
        documentoTexto = normalizarDocumento(texto).take(if (tipoDocumento == "vin") LARGO_VIN else MAX_PLACA)
    }

    /**
     * Foto o PDF elegido para la tarjeta o la baja. Mismas reglas que los documentos de
     * verificación (Constancia): JPG, PNG o PDF de máximo 5 MB; si no sirve se avisa en el campo.
     */
    fun elegirArchivoDocumento(context: Context, uri: Uri) {
        runCatching { Constancia.revisar(context, uri) }
            .onSuccess { archivo ->
                documentoNuevo = archivo
                documentoRuta = null
                documentoArchivo = archivo.uri
                documentoEsPdf = archivo.mime == PDF
                documentoNombre = archivo.nombre
                documentoError = null
            }
            .onFailure { documentoError = it.mensajeUsuario() }
    }

    private fun quitarArchivoDocumento() {
        documentoNuevo = null
        documentoRuta = null
        documentoArchivo = null
        documentoEsPdf = false
        documentoNombre = ""
        documentoError = null
    }

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
        enginePhoto?.let {
            duplicatePhotos = duplicatePhotos - it
            fotosAjenas = fotosAjenas - it
        }
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
            // Huellas de la foto ORIGINAL (antes de tapar placas): así la misma foto en dos
            // recuadros coincide aunque a una se le tapen las placas y a la otra no.
            val exacta = huella(appContext, uri)
            val visual = visuales[uri] ?: HuellaVisual.calcular(appContext, uri)
            val lista = if (taparPlacas) TapaPlacas.tapar(appContext, uri, soloSiHayPlaca) else uri
            huellas[lista] = exacta
            visual?.let { visuales[uri] = it; visuales[lista] = it }
            if (uri in rutasExistentes || uri in yaPublicadas) yaPublicadas += lista
            listo(lista)
            procesando.remove(uri)
        }
    }

    /** Huella visual (HuellaVisual) de la foto original, por Uri (también la de su versión tapada). */
    private val visuales = mutableMapOf<Uri, String>()
    /** Versiones tapadas de fotos que ya estaban publicadas (al editar). */
    private val yaPublicadas = mutableSetOf<Uri>()
    private fun esYaPublicada(uri: Uri) = uri in rutasExistentes || uri in yaPublicadas

    /** Fotos que ya están en una publicación de otra cuenta (fotos_ya_usadas), marcadas en rojo. */
    var fotosAjenas by mutableStateOf<Set<Uri>>(emptySet())
        private set

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

    /** Precio sugerido (paso 3): rango de autos similares; null si no hay suficientes. */
    var sugerenciaPrecio by mutableStateOf<SugerenciaPrecio?>(null)
        private set
    var buscandoPrecio by mutableStateOf(false)
        private set
    /** No se pudo consultar (sin internet): no se muestra nada en vez de "no hay suficientes". */
    var errorPrecio by mutableStateOf(false)
        private set
    private var jobPrecio: Job? = null

    /** [desde]/[hasta] null = el mismo modelo de cualquier año (había pocos de años parecidos). */
    data class SugerenciaPrecio(val rango: PrecioSugerido.Rango, val desde: Int?, val hasta: Int?)

    /**
     * Busca el precio de autos similares: mismo modelo y ±2 años; si hay pocos, el mismo modelo
     * de cualquier año. Con "Otros" (modelo escrito a mano) no se sugiere.
     */
    fun cargarSugerenciaPrecio() {
        // Una búsqueda nueva cancela la anterior (así un resultado viejo no pisa al nuevo).
        jobPrecio?.cancel()
        val modelo = idModelo
        val anioNum = anio.trim().toIntOrNull()
        val anioMax = Calendar.getInstance().get(Calendar.YEAR) + 1
        if (modelo == null || modeloEsOtro || anioNum == null || anioNum !in 1900..anioMax) {
            sugerenciaPrecio = null
            buscandoPrecio = false
            errorPrecio = false
            return
        }
        jobPrecio = viewModelScope.launch {
            buscandoPrecio = true
            sugerenciaPrecio = null
            errorPrecio = false
            sugerenciaPrecio = safeCall {
                var precios = CarRepository.preciosSimilares(modelo, anioNum - 2, anioNum + 2, editId)
                var desde: Int? = anioNum - 2
                var hasta: Int? = anioNum + 2
                if (precios.size < 3) {
                    precios = CarRepository.preciosSimilares(modelo, excluir = editId)
                    desde = null
                    hasta = null
                }
                PrecioSugerido.calcular(precios)?.let { SugerenciaPrecio(it, desde, hasta) }
            }.onFailure { errorPrecio = true }.getOrNull()
            buscandoPrecio = false
        }
    }

    fun usarPrecioTipico() {
        sugerenciaPrecio?.let { precio = it.rango.tipico.toString() }
    }

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
                    .getOrNull()
                    // Documento: si no se puede leer (o es anterior a esta función), el dueño lo vuelve a poner.
                    ?.let { safeCall { CarRepository.documentoVehiculo(id) }.getOrNull()?.let { prefillDocumento(it) } }
                loadingEdit = false
            }
            copyFrom?.let { id ->
                safeCall { CarRepository.ownListing(id) }
                    .onSuccess { a -> if (a != null) prefill(a, copia = true) else error = texto(R.string.publish_edit_not_found) }
                    .onFailure { error = it.mensajeUsuario() }
                loadingEdit = false
            }
        }
    }

    /**
     * Llena el formulario con los datos que tenía la publicación. Con [copia] (Duplicar) es una
     * publicación nueva: sin el motivo de rechazo, el estado ni las fotos de la original.
     */
    private fun prefill(a: AnuncioDto, copia: Boolean = false) {
        if (!copia) {
            rejectedReason = a.motivoRechazo?.takeIf { a.estadoUltimaPropuesta == "rechazada" }
            editingApproved = a.aprobada
        }
        idMarca = a.idMarca
        idModelo = a.idModelo
        if (marcaEsOtro) marcaOtra = a.marca.orEmpty()
        if (modeloEsOtro) modeloOtro = a.modelo.orEmpty()
        anio = a.anio?.toString().orEmpty()
        precio = a.precio?.toLong()?.toString().orEmpty()
        idTransmision = a.idTransmision
        kilometraje = a.kilometraje?.toString().orEmpty()
        recuperadoPorSeguro = a.recuperadoPorSeguro == true
        facturaOriginal = a.facturaOriginal == true
        aireAcondicionado = a.aireAcondicionado == true
        cilindros = a.cilindros?.toString().orEmpty()
        separarMotor(a.cilindrada.orEmpty()).let { (l, asp) -> litros = l; aspiracion = asp }
        combustible = a.combustible.orEmpty()
        idCarroceria = a.idCarroceria
        idColor = a.idColor
        duenos = a.propietariosAnteriores?.toString().orEmpty()
        condicion = a.condicionVehiculo.orEmpty()

        // Al publicar, los caballos de fuerza se agregaron al final de la descripción.
        val desc = a.descripcion.orEmpty()
        val potencia = POTENCIA.find(desc)
        caballos = potencia?.groupValues?.get(1).orEmpty()
        descripcion = potencia?.let { desc.removeRange(it.range) } ?: desc
        imperfecciones = a.descripcionProblemas.orEmpty()

        // Las fotos son de ese auto en particular: al duplicar se eligen de nuevo.
        if (copia) return
        // Las primeras van a los ángulos y el resto a fotos adicionales.
        a.fotos.take(MAX_FOTOS).forEachIndexed { i, ruta ->
            val uri = Supabase.publicUrl(ruta)?.toUri() ?: return@forEachIndexed
            rutasExistentes[uri] = ruta
            if (i < photos.size) photos[i] = uri else extraPhotos += uri
        }
    }

    /**
     * Documento que ya tenía la publicación. Un PDF se muestra con su nombre; una foto, con una URL
     * firmada. Si no se puede firmar, la foto no se precarga (se pide otra) para no dar por puesta
     * una que no se ve.
     */
    private suspend fun prefillDocumento(d: DocumentoVehiculoDto) {
        tipoDocumento = d.tipo
        if (d.tipo in TIPOS_DOCUMENTO_FOTO) {
            if (d.valor.endsWith(".pdf")) {
                documentoRuta = d.valor
                documentoEsPdf = true
                documentoNombre = texto(R.string.publish_document_saved_pdf)
            } else {
                safeCall { CarRepository.documentoVehiculoUrl(d.valor) }.getOrNull()?.let { url ->
                    documentoRuta = d.valor
                    documentoArchivo = url.toUri()
                }
            }
        } else {
            documentoTexto = d.valor
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
        fotosAjenas = fotosAjenas - uri
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
        // Un error de campo se muestra debajo del campo (y se lleva la vista ahí), no arriba.
        val campo = camposDelPaso(step).firstOrNull { errorDe(it) != null }
        if (campo != null) {
            marcarCampo(campo)
            campoAEnfocar = campo
            enfoqueSignal++
        } else {
            // Sin error de campo: al volver a este paso no se debe saltar a un campo ya corregido.
            campoAEnfocar = null
        }
        error = if (campo != null) null else validate(step)
        if (campo == null && error == null && step < 4) step++
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
            val exactas = fotos.map { huella(appContext, it) }
            // Solo se calcula aquí la de fotos ya publicadas que no pasaron por prepararFoto (el
            // tablero al editar); las demás se calcularon del original al elegirlas.
            val vis = fotos.map { u ->
                visuales[u] ?: if (u in rutasExistentes) HuellaVisual.calcular(appContext, u)?.also { visuales[u] = it } else null
            }
            // Repetidas: misma huella del original. Una foto ya publicada (al editar) contra otra
            // se compara por huella visual, porque la guardada ya se tapó y se volvió a guardar.
            val repetidas = mutableSetOf<Uri>()
            for (i in fotos.indices) for (j in i + 1 until fotos.size) {
                val vi = vis[i]
                val vj = vis[j]
                val porVisual = (esYaPublicada(fotos[i]) || esYaPublicada(fotos[j])) &&
                    vi != null && vj != null && HuellaVisual.mismaFoto(vi, vj)
                if (exactas[i] == exactas[j] || porVisual) {
                    repetidas += fotos[i]
                    repetidas += fotos[j]
                }
            }
            duplicatePhotos = repetidas
            fotosAjenas = emptySet()
            if (repetidas.isNotEmpty()) {
                checkingPhotos = false
                error = texto(R.string.publish_error_duplicate_photos)
                photoErrorSignal++
                return@launch
            }
            // Fotos que ya están en una publicación de otra cuenta (solo las nuevas). Si no se
            // pudo revisar (sin internet), no se bloquea: el admin revisa la publicación igual.
            val nuevas = fotos.indices.filter { !esYaPublicada(fotos[it]) && vis[it] != null }
            val posiciones = safeCall { CarRepository.fotosYaUsadas(nuevas.map { vis[it]!! }) }.getOrDefault(emptyList())
            fotosAjenas = posiciones.mapNotNull { nuevas.getOrNull(it)?.let(fotos::get) }.toSet()
            checkingPhotos = false
            if (fotosAjenas.isNotEmpty()) {
                error = texto(R.string.publish_error_photos_used_elsewhere)
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

    /** Mensaje de error de [campo] con lo que tiene ahora, o null si está bien. */
    fun errorDe(campo: String): String? = when (campo) {
        "anio" -> {
            val anioActual = Calendar.getInstance().get(Calendar.YEAR)
            val anioNum = anio.trim().toIntOrNull()
            if (anioNum == null || anioNum !in 1900..(anioActual + 1)) texto(R.string.publish_error_year) else null
        }
        "cilindros" -> when {
            !esElectrico && cilindros.isBlank() -> texto(R.string.publish_error_cylinders_required)
            (cilindros.filter(Char::isDigit).toIntOrNull() ?: 0) > MAX_CILINDROS -> texto(R.string.publish_error_cylinders, MAX_CILINDROS)
            else -> null
        }
        "documento_tipo" -> if (tipoDocumento.isEmpty()) texto(R.string.publish_error_document_type) else null
        "documento" -> if (tipoDocumento.isEmpty()) null
            else errorDocumento(tipoDocumento, documentoTexto, documentoNuevo != null || documentoRuta != null)?.let { texto(it) }
        "descripcion" -> if (descripcion.isBlank()) texto(R.string.publish_error_description) else null
        // El precio va al final del formulario, junto con la sugerencia de autos similares.
        "precio" -> if ((precio.filter(Char::isDigit).toLongOrNull() ?: 0) <= 0) texto(R.string.publish_error_price) else null
        // El primer campo vacío, en el mismo orden del formulario.
        else -> ordenCampos.firstOrNull { it.first == campo }?.takeIf { !it.second }?.let { texto(it.third) }
    }

    /** Campos con validación de cada paso, en el orden del formulario (el 2 son las fotos). */
    private fun camposDelPaso(paso: Int): List<String> = when (paso) {
        1 -> ordenCampos.map { it.first } + listOf("documento_tipo", "documento")
        3 -> listOf("descripcion", "precio")
        else -> emptyList()
    }

    private fun validate(paso: Int): String? = when (paso) {
        2 -> photos.indices.filter { photos[it] == null }
            .takeIf { it.isNotEmpty() }
            ?.let { texto(R.string.publish_error_required_photos, nombresFotos(it)) }
            ?: if (fotosElegidas.size > MAX_FOTOS) texto(R.string.publish_photos_limit, MAX_FOTOS) else null
        else -> camposDelPaso(paso).firstNotNullOfOrNull { errorDe(it) }
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
                // La foto o el PDF del documento va tal cual al bucket privado (sin tapar placas). Se
                // recuerda la ruta para no volver a subirlo si guardar falla y se reintenta.
                val documento = if (!documentoEsFoto) normalizarDocumento(documentoTexto)
                else documentoRuta ?: requireNotNull(documentoNuevo).let { archivo ->
                    CarRepository.uploadDocumento(Constancia.bytes(appContext, archivo), archivo.mime)
                }.also { documentoRuta = it }
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
                    put("factura_original", facturaOriginal)
                    put("aire_acondicionado", aireAcondicionado)
                    put("condicion_vehiculo", condicion)
                    put("id_color", idColor)
                    put("id_transmision", idTransmision)
                    put("id_carroceria", idCarroceria)
                    put("numero_propietarios_anteriores", duenos.filter(Char::isDigit).toIntOrNull() ?: 0)
                    put("cilindros", if (esElectrico) null else cilindros.filter(Char::isDigit).toIntOrNull())
                    put("cilindrada", motor.trim().ifEmpty { null })
                    put("tipo_combustible", combustible.ifEmpty { null })
                    put("descripcion_problemas", imperfecciones.trim())
                    put("tipo_documento_vehiculo", tipoDocumento)
                    put("documento_vehiculo", documento)
                }
                val id = editId
                    ?.let { CarRepository.edit(it, datos, rutas); it }
                    ?: CarRepository.publish(datos, rutas)
                // Huellas visuales de las fotos, para que nadie más publique estas fotos. Solo si
                // están todas (registrar reemplaza las anteriores); si falla no pasa nada.
                val huellasFinales = fotos.mapNotNull { visuales[it] }
                if (huellasFinales.size == fotos.size) {
                    safeCall { CarRepository.registrarHuellas(id, huellasFinales.distinct()) }
                }
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

        /** Valores de condicion_vehiculo (chk_publicaciones_condicion_vehiculo) y su etiqueta. */
        val CONDICIONES = listOf(
            "seminuevo" to R.string.publish_condition_seminuevo,
            "usado" to R.string.publish_condition_usado,
        )

        /** Opciones de combustible; se guardan tal cual en tipo_combustible (catálogo tipos_combustible). */
        val COMBUSTIBLES = listOf("Gasolina", "Diésel", "Híbrido", "Eléctrico")

        /** Combustible que no lleva cilindros ni litros. */
        const val ELECTRICO = "Eléctrico"

        /** Cilindros que se ofrecen en el menú (el máximo es [MAX_CILINDROS]). */
        val CILINDROS = listOf(2, 3, 4, 5, 6, 8, 10, 12, 16)

        /** Año más antiguo del menú de años. */
        const val ANIO_MIN = 1960

        /** El menú de dueños anteriores llega hasta "5 o más" (se guarda 5). */
        const val DUENOS_MAX = 5

        /**
         * Aspiración del motor: valor que se guarda en cilindrada → etiqueta. "Natural" no
         * agrega nada al texto. Biturbo va antes que Turbo para reconocerlo al editar.
         */
        val ASPIRACIONES = listOf(
            "Natural" to R.string.publish_asp_natural,
            "Biturbo" to R.string.publish_asp_biturbo,
            "Turbo" to R.string.publish_asp_turbo,
            "Supercargado" to R.string.publish_asp_super,
        )

        /** Litros al escribir: solo un entero y un decimal; el punto se pone solo ("20" → "2.0"). */
        fun litrosAlEscribir(texto: String): String {
            val d = texto.filter(Char::isDigit).take(2)
            return if (d.length == 2) "${d[0]}.${d[1]}" else d
        }

        /** Litros y aspiración → texto de cilindrada ("2.0 L Turbo"), o vacío. */
        fun unirMotor(litros: String, aspiracion: String): String {
            val l = if (litros.isEmpty()) "" else (if (litros.length == 1) "$litros.0" else litros) + " L"
            val a = if (aspiracion == "Natural") "" else aspiracion
            return listOf(l, a).filter { it.isNotEmpty() }.joinToString(" ")
        }

        /** Cilindrada guardada ("2.0 L Turbo") → litros y aspiración. "V6" no cuenta como litros. */
        fun separarMotor(cilindrada: String): Pair<String, String> {
            val num = Regex("""(?<![\d.,])(\d)[.,](\d)(?!\d)""").find(cilindrada)
                ?: Regex("""(?<![\d.,])(\d)()\s*L\b""", RegexOption.IGNORE_CASE).find(cilindrada)
            val litros = num?.let { "${it.groupValues[1]}.${it.groupValues[2].ifEmpty { "0" }}" }.orEmpty()
            val asp = ASPIRACIONES.map { it.first }
                .firstOrNull { it != "Natural" && cilindrada.contains(it, ignoreCase = true) }
            return litros to (asp ?: if (litros.isNotEmpty()) "Natural" else "")
        }

        /** Tipo normalizado de un documento en PDF (Constancia.mimeValido). */
        private const val PDF = "application/pdf"

        /** Documentos que se dan con foto (JPG o PNG) o PDF; el VIN y la placa se escriben. */
        val TIPOS_DOCUMENTO_FOTO = listOf("tarjeta_circulacion", "baja")

        /** Largo del VIN y máximo de la placa (chk_propuestas_publicacion_documento_vehiculo). */
        const val LARGO_VIN = 17
        const val MAX_PLACA = 10

        /** VIN: 17 letras o números sin I, O ni Q. Placa: 4 a 10 letras, números o guiones. */
        private val VIN = Regex("^[A-HJ-NPR-Z0-9]{17}$")
        private val PLACA = Regex("^[A-Z0-9-]{4,10}$")

        /** VIN o placa sin espacios y en mayúsculas (igual que documento_vehiculo_valido en la BD). */
        fun normalizarDocumento(texto: String): String = texto.filterNot(Char::isWhitespace).uppercase()

        /** Error (recurso de texto) del documento del vehículo, o null si está completo. */
        fun errorDocumento(tipo: String, texto: String, tieneArchivo: Boolean): Int? = when {
            tipo.isEmpty() -> R.string.publish_error_document_type
            tipo == "vin" && !VIN.matches(normalizarDocumento(texto)) -> R.string.publish_error_vin
            tipo == "placa" && !PLACA.matches(normalizarDocumento(texto)) -> R.string.publish_error_plate
            tipo in TIPOS_DOCUMENTO_FOTO && !tieneArchivo -> R.string.publish_error_document_photo
            else -> null
        }

        /** Sufijo "Potencia: N hp" que publish() agrega a la descripción. */
        private val POTENCIA = Regex("""\n\nPotencia: (\d+) hp$""")
    }
}
