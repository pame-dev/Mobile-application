package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonOff
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
import com.pame.karsy.data.repository.AdminStats
import com.pame.karsy.feature.admin.charts.ChartSeries
import com.pame.karsy.feature.admin.charts.GrowthChart
import com.pame.karsy.feature.admin.charts.SalesComparisonChart

private val kpiIcons: List<ImageVector> = listOf(
    Icons.Outlined.Person,
    Icons.Outlined.Storefront,
    Icons.Outlined.DirectionsCar,
    Icons.Outlined.CheckCircle,
    Icons.Outlined.PauseCircle,
)

private val alertIcons: List<ImageVector> = listOf(
    Icons.Outlined.Description,
    Icons.Outlined.PersonOff,
    Icons.Outlined.Storefront,
    Icons.Outlined.Shield,
    Icons.Outlined.Flag,
)

private val rangeOptions = listOf("7", "30", "90")
private fun rangeLabel(range: String) = "Últimos $range días"

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
            "¡Hola, ${SessionManager.account?.nombre ?: "Administrador"}!",
            fontFamily = Outfit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.01).em,
            color = KarsyNavy
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Resumen de la actividad en la plataforma.",
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyMid
        )
        Spacer(Modifier.height(14.dp))
        RangeSelector(range = range, onSelect = vm::changeRange)
        Spacer(Modifier.height(20.dp))

        // KPIs en cuadrícula de 2 columnas (el cambio es lo nuevo en el rango elegido)
        val kpis = vm.kpis
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            kpis.indices.chunked(2).forEach { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowIdx.forEach { i ->
                        KpiCard(kpis[i], kpiIcons[i], Modifier.weight(1f))
                    }
                    if (rowIdx.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        if (stats != null) {
            GrowthCard(stats = stats, range = range, onRange = vm::changeRange)
            Spacer(Modifier.height(16.dp))
            InteractionsCard(stats = stats)
            Spacer(Modifier.height(20.dp))
            AlertsCard(alerts = vm.alerts, onOpen = onNavigate, onSeeAll = { onNavigate(AdminSection.Reportes) })
        }
    }
}

@Composable
private fun RangeSelector(range: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(KarsyWhite)
                .border(1.dp, KarsyBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        ) {
            Icon(
                Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = KarsyTextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                rangeLabel(range),
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = KarsyCharcoal
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = KarsyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = KarsyWhite) {
            rangeOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            rangeLabel(option),
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            color = KarsyCharcoal,
                            fontWeight = if (option == range) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun KpiCard(kpi: AdminKpi, icon: ImageVector, modifier: Modifier = Modifier) {
    AdminCard(modifier = modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(kpi.bg)
            ) {
                Icon(icon, contentDescription = null, tint = kpi.color, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                kpi.label,
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = KarsyMid,
                minLines = 2
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kpi.value,
                    fontFamily = Outfit,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${if (kpi.positive) "↑" else "↓"} ${kpi.change}",
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (kpi.positive) AdminColors.Green else AdminColors.BadgeRed,
                    maxLines = 1
                )
            }
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
private fun GrowthCard(stats: AdminStats, range: String, onRange: (String) -> Unit) {
    AdminCard {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    CardTitle("Crecimiento de la plataforma", "Usuarios, lotes y publicaciones acumulados")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                rangeOptions.forEach { r ->
                    val active = range == r
                    Text(
                        "${r}d",
                        fontFamily = DmSans,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) KarsyWhite else KarsyTextSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) AdminColors.Blue else KarsyWhite)
                            .border(1.dp, KarsyBorder, RoundedCornerShape(8.dp))
                            .clickable { onRange(r) }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LegendItem("Usuarios", UsersColor)
                LegendItem("Lotes", LotsColor)
                LegendItem("Publicaciones", PostsColor)
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
            CardTitle("Interés en publicaciones", "Vistas de detalle vs. contactos al vendedor")
            Spacer(Modifier.height(10.dp))
            Text(
                "$porcentaje% de las vistas terminan en contacto",
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
                LegendItem("Vistas", KarsyTeal, square = true)
                LegendItem("Contactos", KarsyBorderMuted, square = true)
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
                SummaryTile("Vistas", totalVia.toString(), KarsyTealLight, KarsyNavy, Modifier.weight(1f))
                SummaryTile("Contactos", totalFuera.toString(), KarsyBg, KarsyNavy, Modifier.weight(1f))
                SummaryTile("Conversión", "$porcentaje%", Color(0xFFF0FDF4), AdminColors.Green, Modifier.weight(1f))
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
                    "Requiere atención",
                    fontFamily = Outfit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Ver todos →",
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AdminColors.Blue,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                alerts.forEachIndexed { i, alert ->
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
                            Icon(alertIcons[i], contentDescription = null, tint = alert.color, modifier = Modifier.size(16.dp))
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
