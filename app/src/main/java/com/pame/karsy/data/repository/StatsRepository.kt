package com.pame.karsy.data.repository

import com.pame.karsy.core.supabase.Supabase
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/** Métricas del panel del vendedor (función vendedor_estadisticas). */
data class SellerStats(
    val publicadas: Int,
    val activas: Int,
    val vendidas: Int,
    val pendientes: Int,
    val vistas: Int,
    val contactos: Int,
    val vistasMes: Int,
    val vistasMesAnterior: Int,
    val vistasPorMes: List<Float>,
    val favoritosSemana: List<Float>,
    val diasSemana: List<String>,
    /** Totales de las publicaciones ya publicadas: vistas → favoritos → contactos. */
    val embudoVistas: Int,
    val embudoFavoritos: Int,
    val embudoContactos: Int,
    /** Totales por publicación (la más vista primero). */
    val rendimiento: List<RendimientoPublicacion>,
)

/** Datos extra del reporte de Mi panel (vendedor_reporte); solo se piden al exportar. */
data class ReporteExtra(
    /** Precio promedio de autos similares de otros vendedores (misma marca/modelo, año ±2, misma moneda). */
    val mercado: List<MercadoPublicacion> = emptyList(),
    /** Vistas por día de la semana (domingo = 0) y por hora (0–23), hora de México. */
    val porDia: List<Int> = emptyList(),
    val porHora: List<Int> = emptyList(),
    /** Último destacado de cada auto: vistas en los días antes vs. los días que lleva destacado. */
    val destacados: List<DestacadoPublicacion> = emptyList(),
)

data class MercadoPublicacion(val id: Long, val promedio: Double, val similares: Int)

data class DestacadoPublicacion(
    val id: Long,
    val inicio: String,
    val fin: String,
    val dias: Int,
    /** Días exactos de cada ventana (la de "antes" puede ser más corta si el auto se publicó hace poco). */
    val diasAntes: Double,
    val diasDurante: Double,
    val vistasAntes: Int,
    val vistasDurante: Int,
)

data class RendimientoPublicacion(val id: Long, val vistas: Int, val favoritos: Int, val contactos: Int)

/** Indicadores del dashboard de administración (función admin_estadisticas). */
data class AdminStats(
    val usuarios: Int,
    val usuariosNuevos: Int,
    val lotes: Int,
    val lotesNuevos: Int,
    val publicadas: Int,
    val publicadasNuevas: Int,
    val activas: Int,
    val deshabilitadas: Int,
    /** Para "Requiere atención": las que este admin aún no ha visto en Vehículos. */
    val deshabilitadasSinVer: Int,
    val pausadasSinVer: Int,
    val suspendidasSinVer: Int,
    val suspendidas: Int,
    val propuestasPendientes: Int,
    val reportesPendientes: Int,
    val destacadosPendientes: Int,
    val crecimientoEtiquetas: List<String>,
    val crecimientoUsuarios: List<Float>,
    val crecimientoLotes: List<Float>,
    val crecimientoPublicaciones: List<Float>,
    val interaccionesEtiquetas: List<String>,
    val vistasPorMes: List<Int>,
    val contactosPorMes: List<Int>,
)

object StatsRepository {

    suspend fun seller(): SellerStats {
        val j = Supabase.client.postgrest.rpc("vendedor_estadisticas").decodeAs<JsonObject>()
        val embudo = j["embudo"]?.jsonObject ?: JsonObject(emptyMap())
        return SellerStats(
            publicadas = j.int("publicadas"),
            activas = j.int("activas"),
            vendidas = j.int("vendidas"),
            pendientes = j.int("pendientes"),
            vistas = j.int("vistas"),
            contactos = j.int("contactos"),
            vistasMes = j.int("vistas_mes"),
            vistasMesAnterior = j.int("vistas_mes_anterior"),
            vistasPorMes = j.floats("vistas_por_mes"),
            favoritosSemana = j.floats("favoritos_semana"),
            diasSemana = j.strings("dias_semana"),
            embudoVistas = embudo.int("vistas"),
            embudoFavoritos = embudo.int("favoritos"),
            embudoContactos = embudo.int("contactos"),
            rendimiento = j.list("rendimiento").map {
                val r = it.jsonObject
                RendimientoPublicacion(r.getValue("id").jsonPrimitive.long, r.int("vistas"), r.int("favoritos"), r.int("contactos"))
            },
        )
    }

    suspend fun sellerReport(): ReporteExtra {
        val j = Supabase.client.postgrest.rpc("vendedor_reporte").decodeAs<JsonObject>()
        return ReporteExtra(
            mercado = j.list("mercado").map {
                val m = it.jsonObject
                MercadoPublicacion(m.getValue("id").jsonPrimitive.long, m["promedio"]?.jsonPrimitive?.doubleOrNull ?: 0.0, m.int("similares"))
            },
            porDia = j.floats("por_dia").map { it.toInt() },
            porHora = j.floats("por_hora").map { it.toInt() },
            destacados = j.list("destacados").map {
                val d = it.jsonObject
                DestacadoPublicacion(
                    id = d.getValue("id").jsonPrimitive.long,
                    inicio = d["inicio"]?.jsonPrimitive?.content.orEmpty(),
                    fin = d["fin"]?.jsonPrimitive?.content.orEmpty(),
                    dias = d.int("dias").coerceAtLeast(1),
                    diasAntes = d["dias_antes"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    diasDurante = d["dias_durante"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    vistasAntes = d.int("vistas_antes"),
                    vistasDurante = d.int("vistas_durante"),
                )
            },
        )
    }

    suspend fun admin(days: Int): AdminStats {
        val j = Supabase.client.postgrest
            .rpc("admin_estadisticas", buildJsonObject { put("p_dias", days) })
            .decodeAs<JsonObject>()
        val crecimiento = j.getValue("crecimiento").jsonObject
        val interacciones = j.getValue("interacciones").jsonObject
        return AdminStats(
            usuarios = j.int("usuarios"),
            usuariosNuevos = j.int("usuarios_nuevos"),
            lotes = j.int("lotes"),
            lotesNuevos = j.int("lotes_nuevos"),
            publicadas = j.int("publicadas"),
            publicadasNuevas = j.int("publicadas_nuevas"),
            activas = j.int("activas"),
            deshabilitadas = j.int("deshabilitadas"),
            deshabilitadasSinVer = j.int("deshabilitadas_sin_ver"),
            pausadasSinVer = j.int("pausadas_sin_ver"),
            suspendidasSinVer = j.int("suspendidas_sin_ver"),
            suspendidas = j.int("suspendidas"),
            propuestasPendientes = j.int("propuestas_pendientes"),
            reportesPendientes = j.int("reportes_pendientes"),
            destacadosPendientes = j.int("destacados_pendientes"),
            crecimientoEtiquetas = crecimiento.strings("etiquetas"),
            crecimientoUsuarios = crecimiento.floats("usuarios"),
            crecimientoLotes = crecimiento.floats("lotes"),
            crecimientoPublicaciones = crecimiento.floats("publicaciones"),
            interaccionesEtiquetas = interacciones.strings("etiquetas"),
            vistasPorMes = interacciones.floats("vistas").map { it.toInt() },
            contactosPorMes = interacciones.floats("contactos").map { it.toInt() },
        )
    }

    private fun JsonObject.int(key: String): Int = get(key)?.jsonPrimitive?.int ?: 0
    private fun JsonObject.list(key: String): List<JsonElement> = get(key)?.jsonArray.orEmpty()
    private fun JsonObject.floats(key: String): List<Float> = list(key).map { it.jsonPrimitive.int.toFloat() }
    private fun JsonObject.strings(key: String): List<String> = list(key).map { it.jsonPrimitive.content }
}
