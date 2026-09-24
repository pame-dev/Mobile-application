package com.pame.karsy.data.repository

import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.util.Formato
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.data.model.SellerContact
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.ContactoDto
import com.pame.karsy.data.remote.FavoritoDto
import com.pame.karsy.data.remote.InteraccionDto
import com.pame.karsy.data.remote.InteraccionInsert
import com.pame.karsy.data.remote.ReporteInsert
import com.pame.karsy.data.remote.SolicitudDestacadoInsert
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
        }.decodeList<AnuncioDto>().map(::toCar)

    suspend fun detail(id: Long): CarDetail? =
        db.from(VISTA).select { filter { eq("id_publicacion", id) } }
            .decodeSingleOrNull<AnuncioDto>()
            ?.let(::toDetail)

    suspend fun carsByIds(ids: List<Long>): List<Car> {
        if (ids.isEmpty()) return emptyList()
        val porId = db.from(VISTA).select { filter { isIn("id_publicacion", ids) } }
            .decodeList<AnuncioDto>()
            .associateBy { it.idPublicacion }
        return ids.mapNotNull { porId[it]?.let(::toCar) }
    }

    /**
     * Publicaciones de una cuenta. Si es la cuenta en sesión (o el admin) incluye
     * pendientes y rechazadas; para cualquier otro solo las visibles.
     */
    suspend fun ownerCars(ownerId: String, onlyVisible: Boolean = false): List<Car> =
        db.from(VISTA).select {
            filter {
                eq("id_propietario", ownerId)
                if (onlyVisible) eq("visible", true)
            }
            order("fecha_creacion", Order.DESCENDING)
        }.decodeList<AnuncioDto>().map(::toCar)

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
        return carsByIds(ids)
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) {
        val uid = SessionManager.userId ?: throw IllegalStateException("Inicia sesión para guardar favoritos.")
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
            whatsapp = dto.whatsapp == true,
            correo = dto.correo,
        )
    }

    suspend fun report(id: Long, motivo: String) {
        val uid = SessionManager.userId ?: throw IllegalStateException("Inicia sesión para reportar.")
        db.from("reportes").insert(ReporteInsert(id, uid, motivo))
    }

    suspend fun requestFeatured(id: Long) {
        db.from("solicitudes_destacado").insert(SolicitudDestacadoInsert(id))
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

        val autos = carsByIds(vistas.map { it.idPublicacion }).associateBy { it.id }
        return vistas.mapNotNull { v ->
            autos[v.idPublicacion]?.let { it to "Visto ${Formato.haceCuanto(v.fechaHora).replaceFirstChar { c -> c.lowercase() }}" }
        }
    }

    // ── Publicar ─────────────────────────────────────────────────────────────

    /** Sube una foto al bucket y devuelve su ruta (publicaciones/<id_cuenta>/<archivo>). */
    suspend fun uploadPhoto(bytes: ByteArray): String {
        val uid = SessionManager.userId ?: throw IllegalStateException("Inicia sesión para publicar.")
        val ruta = "publicaciones/$uid/${UUID.randomUUID()}.jpg"
        db.storage.from(Supabase.BUCKET).upload(ruta, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        return ruta
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
            brand = a.marca ?: "Sin marca",
            model = a.modelo.orEmpty(),
            year = anio,
            price = Formato.precio(a.precio),
            currency = a.moneda ?: "MXN",
            description = a.descripcion.orEmpty(),
            imageUrl = Supabase.publicUrl(a.fotos.firstOrNull()),
            badge = if (a.destacado) "Destacado" else null,
            condition = if (anio >= anioActual - 3) "Seminuevo" else "Usado",
            kilometraje = Formato.km(a.kilometraje),
            ownerId = a.idPropietario,
            ownerName = a.propietarioNombre,
            status = statusOf(a),
            priceValue = a.precio ?: 0.0,
            brandId = a.idMarca,
            bodyTypeId = a.idCarroceria,
            bodyType = a.carroceria.orEmpty(),
            publishedAt = a.fechaPrimeraPublicacion ?: a.fechaCreacion,
            featured = a.destacado,
            featuredPending = a.destacadoPendiente,
        )
    }

    private fun statusOf(a: AnuncioDto): String = when {
        !a.aprobada && a.estadoUltimaPropuesta == "rechazada" -> "Rechazado"
        !a.aprobada -> "Pendiente"
        a.estadoAdministrativo == "deshabilitada_administrador" -> "Deshabilitado"
        a.estadoPublicacion == "vendida" -> "Vendido"
        a.estadoPublicacion == "deshabilitada" -> "Pausado"
        else -> "Activo"
    }

    private fun toDetail(a: AnuncioDto): CarDetail {
        val car = toCar(a)
        return CarDetail(
            car = car,
            gallery = a.fotos.mapNotNull { Supabase.publicUrl(it) },
            transmision = a.transmision ?: "—",
            kilometraje = Formato.km(a.kilometraje),
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
            detalles = if (a.tieneProblemas == true) a.descripcionProblemas.orEmpty()
            else "Sin problemas ni detalles reportados por el vendedor.",
            descripcionLarga = a.descripcion.orEmpty(),
        )
    }
}
