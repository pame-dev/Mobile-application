package com.pame.karsy.feature.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.locale.textoPlural
import com.pame.karsy.core.supabase.Supabase
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.UserFacingException
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.remote.AnuncioDto
import com.pame.karsy.data.remote.CuentaAdminDto
import com.pame.karsy.data.remote.DestacadoAdminDto
import com.pame.karsy.data.remote.ReporteAdminDto
import com.pame.karsy.data.repository.AdminRepository
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.data.repository.StatsRepository
import kotlinx.coroutines.launch

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

    /** Recarga todo salvo que ya haya una carga en curso. */
    fun refresh() {
        if (!loading) loadAll()
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
                responsible = c.responsable,
                location = listOf(c.municipio, c.estado).filter { it.isNotBlank() }.joinToString(", "),
                activeVehicles = c.activas,
            )
        }

    val vehicles: List<AdminVehicle>
        get() = cars.map { (car, a) ->
            AdminVehicle(
                id = car.id,
                pendingProposalId = a.idUltimaPropuesta.takeIf { a.estadoUltimaPropuesta == "pendiente" },
                name = car.title,
                sellerId = car.ownerId,
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
        get() {
            val porId = cars.associateBy { it.first.id }
            return reportRows.map { r ->
                val car = porId[r.idPublicacion]?.first
                val esCuenta = r.motivo.startsWith("Cuenta del vendedor")
                AdminReport(
                    id = r.idReporte,
                    publicationId = r.idPublicacion,
                    type = if (esCuenta) "Usuario" else "Publicación",
                    reporter = r.reportante,
                    target = r.publicacion,
                    ownerId = car?.ownerId,
                    ownerName = car?.ownerName,
                    imageUrl = car?.imageUrl,
                    reason = r.motivo,
                    resolution = r.motivoResolucion,
                    date = Formato.fechaCorta(r.fecha),
                    status = r.estado.replaceFirstChar { it.uppercase() },
                )
            }
        }

    val featuredRequests: List<FeaturedRequest>
        get() {
            val porId = cars.associateBy { it.first.id }
            return featuredRows.map { s ->
                val (car, a) = porId[s.idPublicacion] ?: (null to null)
                FeaturedRequest(
                    id = s.idSolicitud,
                    publicationId = s.idPublicacion,
                    vehicle = car?.title ?: texto(R.string.admin2_publication_number, s.idPublicacion),
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

    /** Usuarios, lotes y publicados; el estado activas/deshabilitadas va en su propia tarjeta. */
    val kpis: List<AdminKpi>
        get() {
            val s = stats ?: return emptyList()
            return listOf(
                AdminKpi(texto(R.string.admin2_kpi_users), Formato.entero(s.usuarios), "+${s.usuariosNuevos}", Icons.Outlined.Person, Color(0xFF3B82F6), Color(0xFFEFF6FF)),
                AdminKpi(texto(R.string.admin2_kpi_lots), Formato.entero(s.lotes), "+${s.lotesNuevos}", Icons.Outlined.Storefront, Color(0xFFA855F7), Color(0xFFF5F3FF)),
                AdminKpi(texto(R.string.admin2_kpi_published), Formato.entero(s.publicadas), "+${s.publicadasNuevas}", Icons.Outlined.DirectionsCar, Color(0xFF14B8A6), Color(0xFFF0FDFA)),
            )
        }

    val alerts: List<AdminAlert>
        get() {
            val s = stats ?: return emptyList()
            // Solo lo que realmente tiene pendientes.
            return listOf(
                AdminAlert(s.deshabilitadas, textoPlural(R.plurals.admin2_alert_disabled, s.deshabilitadas, s.deshabilitadas), Icons.Outlined.Description, Color(0xFFFEF2F2), Color(0xFFEF4444), AdminSection.Vehiculos),
                AdminAlert(s.suspendidas, textoPlural(R.plurals.admin2_alert_suspended, s.suspendidas, s.suspendidas), Icons.Outlined.PersonOff, Color(0xFFFFFBEB), Color(0xFFF59E0B), AdminSection.Usuarios),
                AdminAlert(s.lotesNuevos, textoPlural(R.plurals.admin2_alert_new_lots, s.lotesNuevos, s.lotesNuevos, range.toInt()), Icons.Outlined.Storefront, Color(0xFFEFF6FF), Color(0xFF3B82F6), AdminSection.Usuarios),
                AdminAlert(s.propuestasPendientes, textoPlural(R.plurals.admin2_alert_pending_review, s.propuestasPendientes, s.propuestasPendientes), Icons.Outlined.Shield, Color(0xFFF5F3FF), Color(0xFF8B5CF6), AdminSection.Vehiculos),
                AdminAlert(s.reportesPendientes, textoPlural(R.plurals.admin2_alert_pending_reports, s.reportesPendientes, s.reportesPendientes), Icons.Outlined.Flag, Color(0xFFFEF2F2), Color(0xFFEF4444), AdminSection.Reportes),
                AdminAlert(s.destacadosPendientes, textoPlural(R.plurals.admin2_alert_pending_featured, s.destacadosPendientes, s.destacadosPendientes), Icons.Outlined.StarOutline, Color(0xFFFFFBEB), Color(0xFFD97706), AdminSection.Destacados),
            ).filter { it.count > 0 }
        }

    // ── Acciones ─────────────────────────────────────────────────────────────

    /** Ejecuta una acción admin; [onSuccess] corre solo si la BD la aceptó. */
    private fun perform(okMessage: String, onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (working) return
        working = true
        viewModelScope.launch {
            safeCall { action() }
                .onSuccess { message = okMessage; loadAll(); onSuccess() }
                .onFailure { message = it.mensajeUsuario() }
            working = false
        }
    }

    fun dismissMessage() {
        message = null
    }

    /** Al suspender, [reason] es obligatorio y queda en la bitácora. */
    fun setSuspended(accountId: String, suspend: Boolean, reason: String? = null, onSuccess: () -> Unit = {}) = perform(
        texto(if (suspend) R.string.admin2_msg_account_suspended else R.string.admin2_msg_account_reactivated),
        onSuccess
    ) {
        AdminRepository.setAccountSuspended(
            accountId, suspend, if (suspend) reason?.trim() else "Reactivada desde el panel de administración."
        )
    }

    /** Al rechazar, [reason] es obligatorio: es lo que verá el vendedor. */
    fun moderate(vehicle: AdminVehicle, approve: Boolean, reason: String? = null, onSuccess: () -> Unit = {}) = perform(
        texto(if (approve) R.string.admin2_msg_publication_approved else R.string.admin2_msg_publication_rejected),
        onSuccess
    ) {
        val id = vehicle.pendingProposalId ?: throw UserFacingException(texto(R.string.admin2_msg_nothing_pending))
        AdminRepository.moderateProposal(id, approve, if (approve) null else reason?.trim())
    }

    /** Al deshabilitar, [reason] es obligatorio: es lo que verá el dueño. */
    fun setVehicleEnabled(vehicle: AdminVehicle, enabled: Boolean, reason: String? = null, onSuccess: () -> Unit = {}) = perform(
        texto(if (enabled) R.string.admin2_msg_publication_enabled else R.string.admin2_msg_publication_disabled),
        onSuccess
    ) {
        AdminRepository.setCarEnabled(vehicle.id, enabled, if (enabled) "Habilitada desde el panel." else reason)
    }

    /** Deshabilita la publicación reportada; [reason] es el comentario que verá el dueño. */
    fun disableFromReport(reportId: Long, reason: String, onSuccess: () -> Unit) = perform(
        texto(R.string.admin2_msg_report_resolved_disabled), onSuccess
    ) {
        AdminRepository.resolveReport(reportId, "atendido", reason.trim(), disableCar = true)
    }

    /** El reporte no procede: la publicación sigue habilitada; [reason] explica por qué. */
    fun dismissReport(reportId: Long, reason: String, onSuccess: () -> Unit) = perform(
        texto(R.string.admin2_msg_report_dismissed), onSuccess
    ) {
        AdminRepository.resolveReport(reportId, "descartado", reason.trim(), disableCar = false)
    }

    /**
     * Reporte de cuenta que procede: suspende al dueño y atiende el reporte con el mismo motivo.
     * Si la cuenta ya estaba suspendida, solo atiende el reporte.
     */
    fun suspendFromReport(report: AdminReport, reason: String, onSuccess: () -> Unit) = perform(
        texto(R.string.admin2_msg_report_resolved_suspended), onSuccess
    ) {
        val ownerId = report.ownerId ?: throw UserFacingException(texto(R.string.admin2_msg_owner_not_found))
        val yaSuspendida = accounts.firstOrNull { it.idCuenta == ownerId }?.estadoCuenta == "suspendida"
        if (!yaSuspendida) AdminRepository.setAccountSuspended(ownerId, true, reason.trim())
        AdminRepository.resolveReport(report.id, "atendido", reason.trim(), disableCar = false)
    }

    fun resolveFeatured(request: FeaturedRequest, approve: Boolean, reason: String? = null) = perform(
        texto(if (approve) R.string.admin2_msg_featured_approved else R.string.admin2_msg_featured_rejected)
    ) {
        AdminRepository.resolveFeatured(request.id, approve, reason)
    }
}

/**
 * Etiqueta traducida de los valores internos del panel (estados, tipos y filtros).
 * Los valores se siguen comparando en español; aquí solo se traduce lo que se muestra.
 */
internal fun adminValueLabelRes(value: String): Int? = when (value) {
    "Todos" -> R.string.admin2_value_all
    "Activo" -> R.string.admin2_value_active
    "Suspendido" -> R.string.admin2_value_suspended
    "Particular" -> R.string.admin2_value_individual
    "Lote" -> R.string.admin2_value_lot
    "Pendiente" -> R.string.admin2_value_pending
    "Cambios pendientes" -> R.string.admin2_value_pending_changes
    "Rechazado" -> R.string.admin2_value_rejected
    "Deshabilitado" -> R.string.admin2_value_disabled
    "Pausado" -> R.string.admin2_value_paused
    "Vendido" -> R.string.admin2_value_sold
    "Atendido" -> R.string.admin2_value_resolved
    "Descartado" -> R.string.admin2_value_dismissed
    "Resuelto" -> R.string.admin2_value_closed
    "Usuario" -> R.string.admin2_value_user
    "Publicación" -> R.string.admin2_value_publication
    else -> null
}

@Composable
internal fun adminValueLabel(value: String): String =
    adminValueLabelRes(value)?.let { stringResource(it) } ?: value
