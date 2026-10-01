package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Formato
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.feature.admin.charts.ChartSeries
import com.pame.karsy.feature.admin.charts.GrowthChart
import com.pame.karsy.feature.admin.charts.SalesComparisonChart

private val rangeOptions = listOf("7", "30", "90")

private val UsersColor = Color(0xFF3B82F6)
private val LotsColor = Color(0xFF8B5CF6)
private val PostsColor = Color(0xFF22C55E)

@Composable
fun AdminHomeSection(vm: AdminViewModel, onNavigate: (AdminSection) -> Unit) {
    val range = vm.range
    val stats = vm.stats

    AdminSectionScroll {
        // Saludo + selector de rango
        Text(
            stringResource(
                R.string.admin2_greeting,
                SessionManager.account?.nombre ?: stringResource(R.string.admin2_administrator)
            ),
            fontFamily = Outfit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.01).em,
            color = KarsyNavy
        )
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.admin2_home_subtitle),
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyMid
        )
        Spacer(Modifier.height(14.dp))
        // Único selector de rango: afecta KPIs, alertas y gráficas
        RangeChips(range = range, onSelect = vm::changeRange)
        Spacer(Modifier.height(20.dp))

        // Pendientes primero: es lo que el admin tiene que atender
        if (stats != null) {
            AlertsCard(alerts = vm.alerts, onOpen = onNavigate, onSeeAll = { onNavigate(AdminSection.Reportes) })
            Spacer(Modifier.height(20.dp))
        }

        // KPIs en cuadrícula 2×2: tres totales + estado de las publicaciones
        val kpis = vm.kpis
        if (stats != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                    kpis.getOrNull(0)?.let { KpiCard(it, Modifier.weight(1f).fillMaxHeight()) }
                    kpis.getOrNull(1)?.let { KpiCard(it, Modifier.weight(1f).fillMaxHeight()) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Max)) {
                    kpis.getOrNull(2)?.let { KpiCard(it, Modifier.weight(1f).fillMaxHeight()) }
                    PublicationStatusCard(stats, Modifier.weight(1f).fillMaxHeight())
                }
            }
            Spacer(Modifier.height(20.dp))

            GrowthCard(stats = stats)
            Spacer(Modifier.height(16.dp))
            InteractionsCard(stats = stats)
        }
    }
}

@Composable
private fun RangeChips(range: String, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(KarsyWhite)
            .border(1.dp, KarsyBorder, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        rangeOptions.forEach { r ->
            val active = range == r
            Text(
                stringResource(R.string.admin2_range_days, r.toInt()),
                fontFamily = DmSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (active) KarsyWhite else KarsyTextSecondary,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) KarsyNavy else Color.Transparent)
                    .clickable(role = Role.Tab) { onSelect(r) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun KpiIcon(icon: ImageVector, color: Color, bg: Color) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun KpiCard(kpi: AdminKpi, modifier: Modifier = Modifier) {
    AdminCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KpiIcon(kpi.icon, kpi.color, kpi.bg)
                Spacer(Modifier.weight(1f))
                kpi.change?.let { change ->
                    Text(
                        change,
                        fontFamily = DmSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BadgeTone.Success.fg,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(BadgeTone.Success.bg)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                kpi.value,
                fontFamily = Outfit,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                kpi.label,
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = KarsyMid
            )
        }
    }
}

/** Activas vs. deshabilitadas en una sola tarjeta, con barra de proporción. */
@Composable
private fun PublicationStatusCard(stats: AdminStats, modifier: Modifier = Modifier) {
    val total = stats.activas + stats.deshabilitadas
    val fraction = if (total == 0) 0f else stats.activas.toFloat() / total
    AdminCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(14.dp)) {
            KpiIcon(Icons.Outlined.CheckCircle, AdminColors.Green, Color(0xFFF0FDF4))
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.admin2_percent, Math.round(fraction * 100)),
                fontFamily = Outfit,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.admin2_kpi_active),
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = KarsyMid
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (total == 0) KarsyBg else Color(0xFFF59E0B).copy(alpha = 0.35f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(AdminColors.Green)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(
                    R.string.admin2_kpi_status_detail,
                    Formato.entero(stats.activas),
                    Formato.entero(stats.deshabilitadas)
                ),
                fontFamily = DmSans,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                color = KarsyTextSecondary
            )
        }
    }
}

@Composable
private fun CardTitle(title: String, subtitle: String) {
    Text(title, fontFamily = Outfit, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KarsyNavy)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, fontFamily = DmSans, fontSize = 12.sp, color = KarsyMid)
}

@Composable
private fun LegendItem(label: String, color: Color, square: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(if (square) 10.dp else 8.dp)
                .clip(if (square) RoundedCornerShape(3.dp) else CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontFamily = DmSans, fontSize = 12.sp, color = KarsyTextSecondary)
    }
}

@Composable
private fun GrowthCard(stats: AdminStats) {
    AdminCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    CardTitle(
                        stringResource(R.string.admin2_growth_title),
                        stringResource(R.string.admin2_growth_subtitle)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendItem(stringResource(R.string.admin2_section_users), UsersColor)
                LegendItem(stringResource(R.string.admin2_section_lots), LotsColor)
                LegendItem(stringResource(R.string.admin2_legend_publications), PostsColor)
            }
            Spacer(Modifier.height(12.dp))
            val maximo = (stats.crecimientoUsuarios + stats.crecimientoPublicaciones + stats.crecimientoLotes)
                .maxOrNull() ?: 0f
            val ticks = niceTicks(maximo)
            GrowthChart(
                labels = stats.crecimientoEtiquetas,
                series = listOf(
                    ChartSeries(stats.crecimientoUsuarios, UsersColor, fillArea = true),
                    ChartSeries(stats.crecimientoPublicaciones, PostsColor, fillArea = true),
                    ChartSeries(stats.crecimientoLotes, LotsColor),
                ),
                maxValue = ticks.last().toFloat(),
                gridValues = ticks,
                modifier = Modifier.fillMaxWidth().height(230.dp)
            )
        }
    }
}

/**
 * Vistas de detalle vs. contactos por mes (interacciones_publicacion).
 * Reemplaza la comparación de ventas del mockup: la BD no registra ventas.
 */
@Composable
private fun InteractionsCard(stats: AdminStats) {
    val via = stats.vistasPorMes
    val fuera = stats.contactosPorMes
    val totalVia = via.sum()
    val totalFuera = fuera.sum()
    val porcentaje = if (totalVia == 0) 0 else Math.round(totalFuera.toFloat() / totalVia * 100)

    AdminCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            CardTitle(
                stringResource(R.string.admin2_interest_title),
                stringResource(R.string.admin2_interest_subtitle)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.admin2_conversion_badge, porcentaje),
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KarsyTealLight)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendItem(stringResource(R.string.admin2_views), KarsyTeal, square = true)
                LegendItem(stringResource(R.string.admin2_contacts), KarsyBorderMuted, square = true)
            }
            Spacer(Modifier.height(14.dp))
            SalesComparisonChart(
                labels = stats.interaccionesEtiquetas,
                viaPlataforma = via,
                fuera = fuera,
                gridValues = niceTicks(((via + fuera).maxOrNull() ?: 0).toFloat()),
                modifier = Modifier.fillMaxWidth().height(230.dp)
            )
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(thickness = 1.dp, color = KarsyBg)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryTile(stringResource(R.string.admin2_views), totalVia.toString(), KarsyTealLight, KarsyNavy, Modifier.weight(1f))
                SummaryTile(stringResource(R.string.admin2_contacts), totalFuera.toString(), KarsyBg, KarsyNavy, Modifier.weight(1f))
                SummaryTile(stringResource(R.string.admin2_conversion), stringResource(R.string.admin2_percent, porcentaje), Color(0xFFF0FDF4), AdminColors.Green, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryTile(label: String, value: String, bg: Color, valueColor: Color, modifier: Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontFamily = DmSans, fontSize = 11.sp, color = KarsyTextSecondary, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, fontFamily = Outfit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
private fun AlertsCard(alerts: List<AdminAlert>, onOpen: (AdminSection) -> Unit, onSeeAll: () -> Unit) {
    AdminCard(shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 14.dp)) {
                Icon(
                    Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.admin2_needs_attention),
                    fontFamily = Outfit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    stringResource(R.string.admin2_see_all),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdminColors.Blue,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                alerts.forEach { alert ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(alert.bg)
                            .clickable { onOpen(alert.target) }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KarsyWhite)
                        ) {
                            Icon(alert.icon, contentDescription = null, tint = alert.color, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            alert.text,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp,
                            color = KarsyCharcoal
                        )
                    }
                }
            }
        }
    }
}
