package com.pame.karsy.data.repository

import com.pame.karsy.R
import com.pame.karsy.data.remote.CuentaIdDto
import com.pame.karsy.data.remote.PrecioDto
import com.pame.karsy.data.remote.PropuestaDto
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import com.pame.karsy.core.util.Traductor
import kotlinx.serialization.json.Json
import com.pame.karsy.data.model.EstadoAuto
import com.pame.karsy.data.remote.TraduccionDto
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.Constancia
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.data.model.SellerContact
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.ContactoDto
import com.pame.karsy.data.remote.DatosPagoDto
import com.pame.karsy.data.remote.DestacadoFinDto
import com.pame.karsy.data.remote.DocumentoVehiculoDto
import com.pame.karsy.data.remote.FavoritoDto
import com.pame.karsy.data.remote.FavoritosConteoDto
import com.pame.karsy.data.remote.FotoPropuestaDto
import com.pame.karsy.data.remote.InteraccionDto
import com.pame.karsy.data.remote.InteraccionInsert
import com.pame.karsy.data.remote.ReporteInsert
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

/**
 * Anuncios desde Supabase. Todas las lecturas usan la vista public.v_anuncios, que
 * respeta el RLS: el público solo ve lo aprobado; el dueño y el admin ven todo lo suyo.
 */
object CarRepository {

    private val db get() = Supabase.client
    private const val VISTA = "v_anuncios"

    /** Imágenes del carrusel de bienvenida (material de marketing, no son anuncios). */
    val welcomeCarousel: List<String> = listOf(
        "1767749995450-7b63ab7cd4fd", "1580273916550-e323be2ae537",
        "1571987502227-9231b837d92a", "1758411898152-5fde4b5eef56",
    ).map { "https://images.unsplash.com/photo-$it?w=600&h=900&fit=crop&auto=format" }

    // ── Lecturas ─────────────────────────────────────────────────────────────

    /** Anuncios aprobados, activos y habilitados, del más reciente al más antiguo. */
    suspend fun visibleCars(): List<Car> =
        db.from(VISTA).select {
            filter { eq("visible", true) }
            order("fecha_primera_publicacion", Order.DESCENDING)
        }.decodeList<AnuncioDto>().map(::toCar).let { withOwnerVerified(it) }

    /** Con [cambiosPendientes] (el admin al revisar) muestra los cambios en revisión, no lo aprobado. */
    suspend fun detail(id: Long, cambiosPendientes: Boolean = false): CarDetail? {
        val detail = db.from(VISTA).select { filter { eq("id_publicacion", id) } }
            .decodeSingleOrNull<AnuncioDto>()
            ?.let { if (cambiosPendientes) conPropuesta(it, setOf("pendiente")) else it }
            ?.let(::toDetail) ?: return null
        // El dueño ve hasta cuándo dura su destacado.
        if (!detail.car.featured || detail.car.ownerId != SessionManager.userId) return detail
        return detail.copy(car = withFeaturedUntil(listOf(detail.car)).first())
    }

    suspend fun carsByIds(ids: List<Long>): List<Car> {
        if (ids.isEmpty()) return emptyList()
        val porId = db.from(VISTA).select { filter { isIn("id_publicacion", ids) } }
            .decodeList<AnuncioDto>()
            .associateBy { it.idPublicacion }
        return ids.mapNotNull { porId[it]?.let(::toCar) }
    }

    /**
     * Precios en pesos de autos del mismo modelo que el público puede ver (activos o vendidos),
     * para sugerir un precio al publicar. Con [desde]/[hasta] solo esos años; [excluir] deja
     * fuera la publicación que se está editando. Igual que preciosSimilares en la web.
     */
    suspend fun preciosSimilares(idModelo: Long, desde: Int? = null, hasta: Int? = null, excluir: Long? = null): List<Double> =
        db.from(VISTA).select(Columns.list("precio")) {
            filter {
                eq("id_modelo", idModelo)
                eq("aprobada", true)
                eq("estado_administrativo", "habilitada")
                isIn("estado_publicacion", listOf("activa", "vendida"))
                eq("moneda", "MXN")
                desde?.let { gte("anio", it) }
                hasta?.let { lte("anio", it) }
                excluir?.let { neq("id_publicacion", it) }
            }
        }.decodeList<PrecioDto>().mapNotNull { it.precio }

    /**
     * Publicaciones de una cuenta. Si es la cuenta en sesión (o el admin) incluye
     * pendientes y rechazadas; para cualquier otro solo las visibles.
     */
    suspend fun ownerCars(ownerId: String, onlyVisible: Boolean = false): List<Car> =
        db.from(VISTA).select {
            filter {
                eq("id_propietario", ownerId)
                // Lo que ve el público en el perfil: activas y vendidas (estas al final).
                if (onlyVisible) {
                    eq("aprobada", true)
                    eq("estado_administrativo", "habilitada")
                    isIn("estado_publicacion", listOf("activa", "vendida"))
                }
            }
            order("fecha_creacion", Order.DESCENDING)
        }.decodeList<AnuncioDto>().map(::toCar)
            .let { if (onlyVisible) it.sortedBy { car -> car.status == EstadoAuto.VENDIDO } else it }
            .let { withFeaturedUntil(it) }

    /**
     * Llena [Car.featuredUntil] con la fecha de fin del destacado vigente (dura 1 mes desde
     * que el admin lo aprueba). Si la consulta falla, las publicaciones se muestran igual.
     */
    private suspend fun withFeaturedUntil(cars: List<Car>): List<Car> {
        val ids = cars.filter { it.featured }.map { it.id }
        if (ids.isEmpty()) return cars
        val fin = runCatching {
            db.from("solicitudes_destacado").select {
                filter {
                    isIn("id_publicacion", ids)
                    eq("estado_solicitud", "aprobada")
                }
            }.decodeList<DestacadoFinDto>()
                .groupBy { it.idPublicacion }
                .mapValues { (_, filas) -> filas.mapNotNull { it.fechaFin }.max() }
        }.getOrDefault(emptyMap())
        return cars.map { car ->
            fin[car.id]?.let {
                car.copy(featuredUntil = Formato.fechaCorta(it), featuredEndsSoon = Formato.terminaEnDias(it, 3))
            } ?: car
        }
    }

    /**
     * Llena [Car.ownerVerified] con cuentas.verificado de los dueños (una consulta para toda la
     * lista; la vista de anuncios no lo trae). Si falla, las tarjetas se muestran sin insignia.
     */
    private suspend fun withOwnerVerified(cars: List<Car>): List<Car> {
        val duenos = cars.map { it.ownerId }.filter { it.isNotEmpty() }.distinct()
        if (duenos.isEmpty()) return cars
        // En bloques: cada id va en la URL de PostgREST y una lista larga la haría demasiado grande.
        val verificados = runCatching {
            duenos.chunked(100).flatMap { bloque ->
                db.from("cuentas").select(Columns.list("id_cuenta")) {
                    filter {
                        isIn("id_cuenta", bloque)
                        eq("verificado", true)
                    }
                }.decodeList<CuentaIdDto>().map { it.idCuenta }
            }.toSet()
        }.getOrDefault(emptySet())
        return cars.map { if (it.ownerId in verificados) it.copy(ownerVerified = true) else it }
    }

    // ── Favoritos ────────────────────────────────────────────────────────────

    suspend fun favoriteIds(): Set<Long> {
        val uid = SessionManager.userId ?: return emptySet()
        return db.from("favoritos").select { filter { eq("id_cuenta", uid) } }
            .decodeList<FavoritoDto>().map { it.idPublicacion }.toSet()
    }

    suspend fun favoriteCars(): List<Car> {
        val uid = SessionManager.userId ?: return emptyList()
        val ids = db.from("favoritos").select {
            filter { eq("id_cuenta", uid) }
            order("fecha_guardado", Order.DESCENDING)
        }.decodeList<FavoritoDto>().map { it.idPublicacion }
        return carsByIds(ids).filter(::sigueDisponible)
    }

    /**
     * Favoritos e historial solo muestran los autos que siguen a la venta (activos): no las
     * vendidas (esas quedan en el perfil del vendedor), pausadas, deshabilitadas, en revisión ni
     * rechazadas, aunque las haya abierto el dueño o el admin. No se borran: si se reactivan,
     * vuelven a aparecer.
     */
    internal fun sigueDisponible(car: Car): Boolean = car.status == EstadoAuto.ACTIVO

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_to_favorite))
        if (favorite) {
            db.from("favoritos").insert(FavoritoDto(uid, id))
        } else {
            db.from("favoritos").delete {
                filter {
                    eq("id_cuenta", uid)
                    eq("id_publicacion", id)
                }
            }
        }
    }

    // ── Interacciones ────────────────────────────────────────────────────────

    /** Registra que se abrió el detalle (alimenta estadísticas e historial). */
    suspend fun registerView(id: Long) {
        runCatching {
            db.from("interacciones_publicacion")
                .insert(InteraccionInsert(id, SessionManager.userId, "ver_detalle"))
        }
    }

    /** Datos de contacto del vendedor; con sesión también registra el contacto. */
    suspend fun contact(id: Long): SellerContact? {
        val dto = db.postgrest.rpc("contacto_vendedor", buildJsonObject { put("p_id_publicacion", id) })
            .decodeAsOrNull<ContactoDto>() ?: return null
        SessionManager.userId?.let { uid ->
            runCatching {
                db.from("interacciones_publicacion").insert(InteraccionInsert(id, uid, "contactar"))
            }
        }
        return SellerContact(
            id = dto.idCuenta,
            nombre = dto.nombre,
            tipoCuenta = dto.tipoCuenta,
            descripcion = dto.descripcion.orEmpty(),
            ubicacion = dto.ubicacion.orEmpty(),
            avatarUrl = Supabase.publicUrl(dto.fotoPerfil),
            telefono = dto.telefono,
            // Antes de la migración contacto_whatsapp solo llega si el teléfono también es WhatsApp.
            whatsapp = dto.numeroWhatsapp ?: dto.telefono?.takeIf { dto.whatsapp == true },
            medioPrincipal = dto.medioPrincipal,
            correo = dto.correo,
        )
    }

    suspend fun report(id: Long, motivo: String) {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_to_report))
        db.from("reportes").insert(ReporteInsert(id, uid, motivo))
    }

    /** Bucket privado de los comprobantes de pago de destacados (dueño y admin). */
    const val BUCKET_COMPROBANTES = "comprobantes-pago"

    /** Precio, días, banco y CLABE para pagar un destacado (función datos_pago_destacado). */
    suspend fun datosPagoDestacado(): DatosPagoDto? =
        db.postgrest.rpc("datos_pago_destacado").decodeList<DatosPagoDto>().firstOrNull()

    /**
     * Sube el comprobante (JPG, PNG o PDF) al bucket privado y devuelve su ruta
     * (<id_cuenta>/<uuid>.<jpg|png|pdf>). [mime] ya viene revisado con Constancia.mimeValido.
     */
    suspend fun uploadComprobante(bytes: ByteArray, mime: String): String {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.featured_pay_error_login))
        val ruta = "$uid/${UUID.randomUUID()}.${Constancia.extension(mime)}"
        db.storage.from(BUCKET_COMPROBANTES).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.parse(mime)
        }
        return ruta
    }

    /**
     * Pide destacar una publicación propia con el comprobante ya subido (función solicitar_destacado:
     * solo lotes). Si ya hay una solicitud pendiente la BD responde 23505 (esDuplicado).
     */
    suspend fun requestFeatured(id: Long, comprobante: String) {
        db.postgrest.rpc("solicitar_destacado", buildJsonObject {
            put("p_id_publicacion", id)
            put("p_comprobante", comprobante)
        })
    }

    /** Anuncios vistos por la cuenta en sesión, con el texto "Visto hace …". */
    suspend fun history(): List<Pair<Car, String>> {
        val uid = SessionManager.userId ?: return emptyList()
        val vistas = db.from("interacciones_publicacion").select {
            filter {
                eq("id_cuenta", uid)
                eq("tipo_interaccion", "ver_detalle")
            }
            order("fecha_hora", Order.DESCENDING)
            limit(200)
        }.decodeList<InteraccionDto>().distinctBy { it.idPublicacion }

        val autos = carsByIds(vistas.map { it.idPublicacion }).filter(::sigueDisponible).associateBy { it.id }
        return vistas.mapNotNull { v ->
            autos[v.idPublicacion]?.let { it to texto(R.string.core_history_seen, Formato.haceCuanto(v.fechaHora).replaceFirstChar { c -> c.lowercase() }) }
        }
    }

    // ── Publicar ─────────────────────────────────────────────────────────────

    /** Sube una foto al bucket y devuelve su ruta (publicaciones/<id_cuenta>/<archivo>). */
    suspend fun uploadPhoto(bytes: ByteArray): String {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_to_publish))
        val ruta = "publicaciones/$uid/${UUID.randomUUID()}.jpg"
        db.storage.from(Supabase.BUCKET).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        return ruta
    }

    /** Bucket privado de las fotos o PDF de tarjeta de circulación y documento de baja. */
    const val BUCKET_DOCUMENTOS = "documentos-vehiculo"

    /** Valores de propuestas_publicacion.tipo_documento_vehiculo. */
    val TIPOS_DOCUMENTO = listOf("vin", "placa", "tarjeta_circulacion", "baja")

    /**
     * Sube la foto (JPG o PNG) o el PDF del documento del vehículo al bucket privado y devuelve su
     * ruta (<id_cuenta>/<uuid>.<jpg|png|pdf>). [mime] ya viene revisado con Constancia.mimeValido.
     */
    suspend fun uploadDocumento(bytes: ByteArray, mime: String): String {
        val uid = SessionManager.userId ?: throw UserFacingException(texto(R.string.core_error_login_to_publish))
        val ruta = "$uid/${UUID.randomUUID()}.${Constancia.extension(mime)}"
        db.storage.from(BUCKET_DOCUMENTOS).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.parse(mime)
        }
        return ruta
    }

    /**
     * Documento del vehículo de la propuesta más reciente que lo tenga; null si nunca se dio.
     * Solo lo pueden leer el dueño y el admin (RLS de propuestas_publicacion).
     */
    suspend fun documentoVehiculo(idPublicacion: Long): DocumentoVehiculoDto? =
        db.from("propuestas_publicacion").select(Columns.list("tipo_documento_vehiculo", "documento_vehiculo")) {
            filter {
                eq("id_publicacion", idPublicacion)
                isIn("tipo_documento_vehiculo", TIPOS_DOCUMENTO)
            }
            order("fecha_envio_moderacion", Order.DESCENDING)
            order("id_propuesta", Order.DESCENDING)
            limit(1)
        }.decodeList<DocumentoVehiculoDto>().firstOrNull()

    /** URL firmada y temporal (10 min) de la foto o el PDF de un documento del vehículo. */
    suspend fun documentoVehiculoUrl(ruta: String): String =
        db.storage.from(BUCKET_DOCUMENTOS).createSignedUrl(ruta, 10.minutes)

    /**
     * Datos de una publicación propia para precargar el formulario al corregirla. Si tiene
     * cambios en revisión o rechazados, se toman de esa propuesta (la que editar_publicacion
     * vuelve a actualizar): en una ya aprobada, v_anuncios sigue mostrando la versión aprobada.
     */
    suspend fun ownListing(id: Long): AnuncioDto? =
        db.from(VISTA).select { filter { eq("id_publicacion", id) } }.decodeSingleOrNull<AnuncioDto>()
            ?.let { conPropuesta(it, setOf("pendiente", "rechazada")) }

    /**
     * [a] con los datos del vehículo y las fotos de su última propuesta, si está en uno de
     * [estados]; si no, [a] tal cual. Solo el dueño y el admin pueden leer la propuesta (RLS).
     */
    private suspend fun conPropuesta(a: AnuncioDto, estados: Set<String>): AnuncioDto {
        val idPropuesta = a.idUltimaPropuesta?.takeIf { a.estadoUltimaPropuesta in estados } ?: return a
        val p = db.from("propuestas_publicacion").select(Columns.raw(COLUMNAS_PROPUESTA)) {
            filter { eq("id_propuesta", idPropuesta) }
        }.decodeSingleOrNull<PropuestaDto>() ?: return a
        val fotos = db.from("fotos_propuesta_publicacion").select(Columns.list("ruta_storage")) {
            filter { eq("id_propuesta", idPropuesta) }
            order("orden_foto", Order.ASCENDING)
        }.decodeList<FotoPropuestaDto>().map { it.ruta }
        return a.copy(
            titulo = p.titulo,
            descripcion = p.descripcion,
            precio = p.precio,
            idMarca = p.idMarca,
            marca = p.marcaOtra ?: p.marcas?.nombre,
            idModelo = p.idModelo,
            modelo = p.modeloOtro ?: p.modelos?.nombre,
            anio = p.anio,
            kilometraje = p.kilometraje,
            idColor = p.idColor,
            color = p.colorOtro ?: p.colores?.nombre,
            idTransmision = p.idTransmision,
            transmision = p.transmisionOtra ?: p.transmisiones?.nombre,
            idCarroceria = p.idCarroceria,
            carroceria = p.carroceriaOtra ?: p.carrocerias?.nombre,
            propietariosAnteriores = p.propietariosAnteriores,
            tieneProblemas = p.tieneProblemas,
            descripcionProblemas = p.descripcionProblemas,
            recuperadoPorSeguro = p.recuperadoPorSeguro,
            facturaOriginal = p.facturaOriginal,
            aireAcondicionado = p.aireAcondicionado,
            condicionVehiculo = p.condicionVehiculo,
            combustible = p.combustible,
            cilindros = p.cilindros,
            cilindrada = p.cilindrada,
            fotos = fotos.ifEmpty { a.fotos },
        )
    }

    /** Columnas de propuestas_publicacion que lee [conPropuesta] (las de [PropuestaDto]). */
    private const val COLUMNAS_PROPUESTA =
        "titulo, descripcion, precio, id_marca, marca_otra, id_modelo, modelo_otro, anio, kilometraje, " +
            "id_color, color_otro, id_transmision, transmision_otra, id_carroceria, carroceria_otra, " +
            "numero_propietarios_anteriores, tiene_problemas, descripcion_problemas, recuperado_por_seguro, " +
            "factura_original, aire_acondicionado, condicion_vehiculo, tipo_combustible, cilindros, cilindrada, " +
            "marcas(nombre_marca), modelos(nombre_modelo), colores(nombre_color), " +
            "transmisiones(nombre_transmision), carrocerias(nombre_carroceria)"

    /**
     * Edita una publicación propia (o corrige una rechazada): los cambios quedan
     * en revisión hasta que un admin los apruebe (función editar_publicacion).
     */
    suspend fun edit(id: Long, datos: JsonObject, fotos: List<String>) {
        db.postgrest.rpc("editar_publicacion", buildJsonObject {
            put("p_id_publicacion", id)
            put("p_datos", datos)
            put("p_fotos", buildJsonArray { fotos.forEach { add(it) } })
        })
    }

    /** Pausa (deshabilita) o reactiva una publicación propia; no afecta lo que decida el admin. */
    suspend fun setPaused(id: Long, paused: Boolean) {
        db.from("publicaciones").update({ set("estado_publicacion", if (paused) "deshabilitada" else "activa") }) {
            filter { eq("id_publicacion", id) }
        }
    }

    /**
     * Marca una publicación propia como vendida ([via]: "dentro_app" o "fuera_app") o le
     * quita la marca para que vuelva a estar disponible (función marcar_publicacion_vendida).
     */
    suspend fun setSold(id: Long, sold: Boolean, via: String? = null) {
        db.postgrest.rpc("marcar_publicacion_vendida", buildJsonObject {
            put("p_id_publicacion", id)
            put("p_vendida", sold)
            put("p_medio_venta", via)
        })
    }

    /** Cuántos usuarios guardaron en favoritos cada publicación de la cuenta en sesión. */
    suspend fun favoriteCounts(): Map<Long, Int> =
        db.postgrest.rpc("favoritos_mis_publicaciones").decodeList<FavoritosConteoDto>()
            .associate { it.idPublicacion to it.total }

    /**
     * Posiciones (desde 0) de las huellas visuales que ya están en una publicación de otra
     * cuenta (función fotos_ya_usadas). Igual que fotosYaUsadas en la web.
     */
    suspend fun fotosYaUsadas(huellas: List<String>): List<Int> {
        if (huellas.isEmpty()) return emptyList()
        return db.postgrest.rpc("fotos_ya_usadas", buildJsonObject {
            put("p_huellas", buildJsonArray { huellas.forEach { add(it) } })
        }).decodeAs<List<Int>>().map { it - 1 }
    }

    /** Guarda las huellas visuales de las fotos de una publicación propia (al publicar o editar). */
    suspend fun registrarHuellas(idPublicacion: Long, huellas: List<String>) {
        db.postgrest.rpc("registrar_huellas_fotos", buildJsonObject {
            put("p_id_publicacion", idPublicacion)
            put("p_huellas", buildJsonArray { huellas.forEach { add(it) } })
        })
    }

    /** Crea la publicación con su propuesta pendiente (función crear_publicacion). */
    suspend fun publish(datos: JsonObject, fotos: List<String>): Long =
        db.postgrest.rpc("crear_publicacion", buildJsonObject {
            put("p_datos", datos)
            put("p_fotos", buildJsonArray { fotos.forEach { add(it) } })
        }).decodeAs<Long>()

    // ── Mapeo ────────────────────────────────────────────────────────────────

    fun toCar(a: AnuncioDto): Car {
        val anioActual = Calendar.getInstance().get(Calendar.YEAR)
        val anio = a.anio ?: 0
        return Car(
            id = a.idPublicacion,
            brand = a.marca ?: texto(R.string.core_car_no_brand),
            model = a.modelo.orEmpty(),
            year = anio,
            price = Formato.precio(a.precio),
            currency = a.moneda ?: "MXN",
            description = a.descripcion.orEmpty(),
            imageUrl = Supabase.publicUrl(a.fotos.firstOrNull()),
            badge = if (a.destacado) texto(R.string.core_car_badge_featured) else null,
            condition = if (anio >= anioActual - 3) texto(R.string.core_car_condition_like_new) else texto(R.string.core_car_condition_used),
            condicion = a.condicionVehiculo,
            kilometraje = Formato.km(a.kilometraje),
            ownerId = a.idPropietario,
            ownerName = a.propietarioNombre,
            sellerType = a.propietarioTipo,
            status = statusOf(a),
            priceValue = a.precio ?: 0.0,
            brandId = a.idMarca,
            bodyTypeId = a.idCarroceria,
            bodyType = a.carroceria.orEmpty(),
            publishedAt = a.fechaPrimeraPublicacion ?: a.fechaCreacion,
            featured = a.destacado,
            featuredPending = a.destacadoPendiente,
            estado = a.estadoUbicacion.orEmpty(),
            municipio = a.municipioUbicacion.orEmpty(),
            disabledReason = a.motivoDeshabilitacion
                ?.takeIf { a.estadoAdministrativo == "deshabilitada_administrador" },
            rejectedReason = a.motivoRechazo?.takeIf { a.estadoUltimaPropuesta == "rechazada" },
            inReview = a.estadoUltimaPropuesta == "pendiente",
            approved = a.aprobada,
            sold = a.vendido || a.estadoPublicacion == "vendida",
            soldVia = a.medioVenta,
        )
    }

    private fun statusOf(a: AnuncioDto): String = when {
        !a.aprobada && a.estadoUltimaPropuesta == "rechazada" -> EstadoAuto.RECHAZADO
        !a.aprobada -> EstadoAuto.PENDIENTE
        a.estadoAdministrativo == "deshabilitada_administrador" -> EstadoAuto.DESHABILITADO
        a.estadoPublicacion == "vendida" -> EstadoAuto.VENDIDO
        a.estadoPublicacion == "deshabilitada" -> EstadoAuto.PAUSADO
        else -> EstadoAuto.ACTIVO
    }

    private fun toDetail(a: AnuncioDto): CarDetail {
        val car = toCar(a)
        return CarDetail(
            car = car,
            gallery = a.fotos.mapNotNull { Supabase.publicUrl(it) },
            transmision = a.transmision ?: "—",
            kilometraje = Formato.km(a.kilometraje),
            recuperadoPorSeguro = a.recuperadoPorSeguro?.let { texto(if (it) R.string.publish_yes else R.string.publish_no) } ?: "—",
            facturaOriginal = a.facturaOriginal?.let { texto(if (it) R.string.publish_yes else R.string.publish_no) } ?: "—",
            aireAcondicionado = a.aireAcondicionado?.let { texto(if (it) R.string.publish_yes else R.string.publish_no) } ?: "—",
            condicion = when (a.condicionVehiculo) {
                "seminuevo" -> texto(R.string.publish_condition_seminuevo)
                "usado" -> texto(R.string.publish_condition_usado)
                else -> "—"
            },
            cilindros = a.cilindros?.toString() ?: "—",
            motor = a.cilindrada ?: "—",
            tipoCarro = a.carroceria ?: "—",
            color = a.color ?: "—",
            cantDuenos = a.propietariosAnteriores?.toString() ?: "—",
            combustible = a.combustible ?: "—",
            ubicacion = listOfNotNull(a.municipioUbicacion, a.estadoUbicacion).joinToString(", "),
            sellerId = a.idPropietario,
            sellerType = a.propietarioTipo,
            sellerAvatar = Supabase.publicUrl(a.propietarioFoto),
            contactoNombre = a.propietarioNombre,
            tieneProblemas = a.tieneProblemas == true && !a.descripcionProblemas.isNullOrBlank(),
            detalles = if (a.tieneProblemas == true) a.descripcionProblemas.orEmpty()
            else texto(R.string.core_car_no_issues),
            descripcionLarga = a.descripcion.orEmpty(),
        )
    }

    /**
     * Descripción e imperfecciones en inglés (traducción automática gratuita, Traductor).
     * null si no hay nada que traducir o no se pudo; cada parte es null si falló o venía vacía.
     */
    suspend fun traduccion(descripcion: String, problemas: String): TraduccionDto? = coroutineScope {
        val d = async { Traductor.alIngles(descripcion) }
        val p = async { Traductor.alIngles(problemas) }
        TraduccionDto(d.await(), p.await()).takeIf { it.descripcion != null || it.problemas != null }
    }
}
