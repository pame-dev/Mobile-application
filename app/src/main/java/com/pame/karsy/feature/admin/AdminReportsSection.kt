package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

@Composable
fun AdminReportsSection(vm: AdminViewModel, onCarClick: (Long) -> Unit) {
    var status by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminReport?>(null) }

    val reports = vm.reports
    val filtered = reports.filter { status == "Todos" || it.status == status }

    AdminSectionScroll {
        AdminSectionHeader(
            title = "Reportes",
            description = "Revisa incidencias y reportes enviados por la comunidad de Karsy."
        )
        AdminCard {
            AdminFilterBar {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Incidencias",
                            fontFamily = Outfit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KarsyNavy
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "${reports.size} reportes registrados",
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            color = AdminColors.Muted
                        )
                    }
                    AdminSelect(
                        value = status,
                        options = listOf("Todos", "Pendiente", "Atendido", "Descartado"),
                        onSelect = { status = it }
                    )
                }
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Outlined.Flag,
                    title = "Sin reportes",
                    description = "No hay reportes con este estado."
                )
            } else {
                filtered.forEachIndexed { index, report ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminBadge(report.type, if (report.type == "Usuario") BadgeTone.Info else BadgeTone.Neutral)
                            Spacer(Modifier.weight(1f))
                            AdminBadge(report.status, reportStatusTone(report.status))
                        }
                        Text(
                            report.target,
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = KarsyNavy
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField("Reportado por", report.reporter, Modifier.weight(1.3f))
                            AdminField("Fecha", report.date, Modifier.weight(1f))
                            AdminActionButton("Revisar", onClick = { selected = report })
                        }
                    }
                }
            }
        }
    }

    selected?.let { report ->
        AdminDetailModal(
            title = "Revisión del reporte",
            subtitle = "Reporte #${report.id.toString().padStart(4, '0')}",
            onDismiss = { selected = null }
        ) {
            DetailRow("Tipo", report.type)
            DetailRow("Reportado por", report.reporter)
            DetailRow("Publicación", report.target)
            DetailRow("Motivo", report.reason)
            DetailRow("Estado", report.status)
            report.resolution?.let { DetailRow("Resolución", it) }
            // Solo un reporte pendiente se puede resolver (admin_resolver_reporte).
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 18.dp)
            ) {
                if (report.status == "Pendiente") {
                    AdminActionButton("Marcar atendido", tone = ActionTone.Success, onClick = {
                        vm.resolveReport(report, disableCar = false)
                        selected = null
                    })
                    AdminActionButton("Deshabilitar", tone = ActionTone.Danger, onClick = {
                        vm.resolveReport(report, disableCar = true)
                        selected = null
                    })
                    AdminActionButton("Descartar", onClick = {
                        vm.dismissReport(report)
                        selected = null
                    })
                } else {
                    AdminActionButton("Ver publicación", onClick = {
                        selected = null
                        onCarClick(report.publicationId)
                    })
                }
            }
        }
    }
}

private fun reportStatusTone(status: String) = when (status) {
    "Atendido" -> BadgeTone.Success
    "Descartado" -> BadgeTone.Neutral
    else -> BadgeTone.Danger
}
