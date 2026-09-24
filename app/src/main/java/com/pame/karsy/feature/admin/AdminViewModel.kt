package com.pame.karsy.feature.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.CuentaAdminDto
import com.pame.karsy.data.remote.DestacadoAdminDto
import com.pame.karsy.data.remote.ReporteAdminDto
import com.pame.karsy.data.repository.AdminRepository
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.data.repository.StatsRepository
import com.pame.karsy.data.model.Car
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Datos y acciones del panel de administración. Todo sale de Supabase; las acciones
 * llaman a las funciones admin_* de la BD, que validan el rol y registran la bitácora.
 */
class AdminViewModel : ViewModel() {

    var stats by mutableStateOf<AdminStats?>(null)
        private set
    var range by mutableStateOf("7")
        private set

    private var accounts by mutableStateOf<List<CuentaAdminDto>>(emptyList())
    private var cars by mutableStateOf<List<Pair<Car, AnuncioDto>>>(emptyList())
    private var reportRows by mutableStateOf<List<ReporteAdminDto>>(emptyList())
    private var featuredRows by mutableStateOf<List<DestacadoAdminDto>>(emptyList())

    var loading by mutableStateOf(true)
        private set
    /** Resultado de la última acción (o error) para mostrar en pantalla. */
    var message by mutableStateOf<String?>(null)
    var working by mutableStateOf(false)
        private set

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            loading = true
            val errores = mutableListOf<String>()
            safeCall { StatsRepository.admin(range.toInt()) }.onSuccess { stats = it }.onFailure { errores += it.mensajeUsuario() }
            safeCall { AdminRepository.accounts() }.onSuccess { accounts = it }.onFailure { errores += it.mensajeUsuario() }
            safeCall { AdminRepository.cars() }.onSuccess { cars = it }.onFailure { errores += it.mensajeUsuario() }
            safeCall { AdminRepository.reports() }.onSuccess { reportRows = it }.onFailure { errores += it.mensajeUsuario() }
            safeCall { AdminRepository.featuredRequests() }.onSuccess { featuredRows = it }.onFailure { errores += it.mensajeUsuario() }
            if (errores.isNotEmpty()) message = errores.first()
            loading = false
        }
    }

    fun changeRange(newRange: String) {
        if (newRange == range) return
        range = newRange
        viewModelScope.launch {
            safeCall { StatsRepository.admin(newRange.toInt()) }
                .onSuccess { stats = it }
                .onFailure { message = it.mensajeUsuario() }
        }
    }

    // ── Listas para las secciones ────────────────────────────────────────────

    val users: List<AdminUser>
        get() = accounts.map { c ->
            AdminUser(
                id = c.idCuenta,
                name = c.nombre,
                email = c.correo,
                type = if (c.tipoCuenta == "lote") "Lote" else "Particular",
                date = Formato.fechaCorta(c.fechaCreacion),
                status = if (c.estadoCuenta == "suspendida") "Suspendido" else "Activo",
                posts = c.publicaciones,
                isAdmin = c.esAdmin,
            )
        }

    val lots: List<AdminLot>
        get() = accounts.filter { it.tipoCuenta == "lote" }.map { c ->
            AdminLot(
                id = c.idCuenta,
                name = c.nombre,
                responsible = c.responsable,
                city = "${c.municipio}, ${c.estado}",
                vehicles = c.activas,
                status = if (c.estadoCuenta == "suspendida") "Suspendido" else "Activo",
            )
        }

    val vehicles: List<AdminVehicle>
        get() = cars.map { (car, a) ->
            AdminVehicle(
                id = car.id,
                pendingProposalId = a.idUltimaPropuesta.takeIf { a.estadoUltimaPropuesta == "pendiente" },
                name = car.title,
                seller = car.ownerName,
                year = car.year,
                price = car.price,
                status = if (a.aprobada && a.estadoUltimaPropuesta == "pendiente") "Cambios pendientes" else car.status,
                transmision = a.transmision ?: "—",
                kilometraje = Formato.km(a.kilometraje),
                tipo = a.carroceria ?: "—",
                color = a.color ?: "—",
                imageUrl = car.imageUrl,
                enabledByAdmin = a.estadoAdministrativo == "habilitada",
            )
        }

    val reports: List<AdminReport>
        get() = reportRows.map { r ->
            val esCuenta = r.motivo.startsWith("Cuenta del vendedor")
            AdminReport(
                id = r.idReporte,
                publicationId = r.idPublicacion,
                type = if (esCuenta) "Usuario" else "Publicación",
                reporter = r.reportante,
                target = r.publicacion,
                reason = r.motivo,
                resolution = r.motivoResolucion,
                date = Formato.fechaCorta(r.fecha),
                status = r.estado.replaceFirstChar { it.uppercase() },
            )
        }

    val featuredRequests: List<FeaturedRequest>
        get() {
            val porId = cars.associateBy { it.first.id }
            return featuredRows.map { s ->
                val (car, a) = porId[s.idPublicacion] ?: (null to null)
                FeaturedRequest(
                    id = s.idSolicitud,
                    publicationId = s.idPublicacion,
                    vehicle = car?.title ?: "Publicación #${s.idPublicacion}",
                    year = car?.year ?: 0,
                    price = car?.let { "${it.price} ${it.currency}" } ?: "—",
                    image = car?.imageUrl,
                    user = s.solicitante,
                    userAvatar = Supabase.publicUrl(s.solicitanteFoto),
                    time = Formato.haceCuanto(s.fecha),
                    status = when (s.estado) {
                        "aprobada" -> FeaturedStatus.Aprobada
                        "rechazada" -> FeaturedStatus.Rechazada
                        else -> FeaturedStatus.Pendiente
                    },
                    transmision = a?.transmision ?: "—",
                    kilometraje = Formato.km(a?.kilometraje),
                    color = a?.color ?: "—",
                    combustible = a?.combustible ?: "—",
                    description = a?.descripcion.orEmpty(),
                )
            }
        }

    val kpis: List<AdminKpi>
        get() {
            val s = stats ?: return emptyList()
            val pctActivas = if (s.publicadas == 0) 0 else (s.activas * 100f / s.publicadas).roundToInt()
            return listOf(
                AdminKpi("Usuarios registrados", Formato.entero(s.usuarios), "+${s.usuariosNuevos}", true, Color(0xFF3B82F6), Color(0xFFEFF6FF)),
                AdminKpi("Lotes registrados", Formato.entero(s.lotes), "+${s.lotesNuevos}", true, Color(0xFFA855F7), Color(0xFFF5F3FF)),
                AdminKpi("Vehículos publicados", Formato.entero(s.publicadas), "+${s.publicadasNuevas}", true, Color(0xFF14B8A6), Color(0xFFF0FDFA)),
                AdminKpi("Publicaciones activas", Formato.entero(s.activas), "$pctActivas%", true, Color(0xFF22C55E), Color(0xFFF0FDF4)),
                AdminKpi("Publicaciones deshabilitadas", Formato.entero(s.deshabilitadas), "${s.deshabilitadas}", false, Color(0xFFF59E0B), Color(0xFFFFFBEB)),
            )
        }

    val alerts: List<AdminAlert>
        get() {
            val s = stats ?: return emptyList()
            return listOf(
                AdminAlert("${s.deshabilitadas} publicaciones deshabilitadas", Color(0xFFFEF2F2), Color(0xFFEF4444), AdminSection.Vehiculos),
                AdminAlert("${s.suspendidas} cuentas suspendidas", Color(0xFFFFFBEB), Color(0xFFF59E0B), AdminSection.Usuarios),
                AdminAlert("${s.lotesNuevos} lotes nuevos en ${range} días", Color(0xFFEFF6FF), Color(0xFF3B82F6), AdminSection.Lotes),
                AdminAlert("${s.propuestasPendientes} publicaciones pendientes de revisión", Color(0xFFF5F3FF), Color(0xFF8B5CF6), AdminSection.Vehiculos),
                AdminAlert("${s.reportesPendientes} reportes pendientes", Color(0xFFFEF2F2), Color(0xFFEF4444), AdminSection.Reportes),
            )
        }

    // ── Acciones ─────────────────────────────────────────────────────────────

    private fun perform(okMessage: String, action: suspend () -> Unit) {
        if (working) return
        working = true
        viewModelScope.launch {
            safeCall { action() }
                .onSuccess { message = okMessage; loadAll() }
                .onFailure { message = it.mensajeUsuario() }
            working = false
        }
    }

    fun setSuspended(accountId: String, suspend: Boolean) = perform(
        if (suspend) "Cuenta suspendida." else "Cuenta reactivada."
    ) {
        AdminRepository.setAccountSuspended(
            accountId, suspend, if (suspend) "Suspendida desde el panel de administración." else "Reactivada desde el panel de administración."
        )
    }

    fun moderate(vehicle: AdminVehicle, approve: Boolean) = perform(
        if (approve) "Publicación aprobada." else "Publicación rechazada."
    ) {
        val id = vehicle.pendingProposalId ?: throw IllegalStateException("No hay nada pendiente de revisar.")
        AdminRepository.moderateProposal(id, approve, if (approve) null else "No cumple con las reglas de publicación.")
    }

    fun setVehicleEnabled(vehicle: AdminVehicle, enabled: Boolean) = perform(
        if (enabled) "Publicación habilitada." else "Publicación deshabilitada."
    ) {
        AdminRepository.setCarEnabled(
            vehicle.id, enabled, if (enabled) "Habilitada desde el panel." else "Deshabilitada desde el panel."
        )
    }

    fun resolveReport(report: AdminReport, disableCar: Boolean) = perform(
        if (disableCar) "Reporte atendido y publicación deshabilitada." else "Reporte marcado como atendido."
    ) {
        AdminRepository.resolveReport(
            report.id,
            "atendido",
            if (disableCar) "Se deshabilitó la publicación reportada." else "Reporte revisado por administración.",
            disableCar
        )
    }

    fun dismissReport(report: AdminReport) = perform("Reporte descartado.") {
        AdminRepository.resolveReport(report.id, "descartado", "El reporte no procede.", false)
    }

    fun resolveFeatured(request: FeaturedRequest, approve: Boolean, reason: String? = null) = perform(
        if (approve) "Publicación destacada por 1 mes." else "Solicitud rechazada."
    ) {
        AdminRepository.resolveFeatured(request.id, approve, reason)
    }
}
