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
fun AdminReportsSection() {
    var status by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminReport?>(null) }

    // TODO: cargar reportes enviados por la comunidad desde Supabase
    val reports = AdminSampleData.reports
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
                        options = listOf("Todos", "Pendiente", "En revisión", "Resuelto"),
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
            DetailRow("Elemento", report.target)
            DetailRow("Estado", report.status)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 18.dp)
            ) {
                AdminActionButton("Marcar resuelto", tone = ActionTone.Success, onClick = {
                    // TODO: resolver reportes: actualizar estado del reporte a 'resuelto'
                    //  + insertar en acciones_administrativas
                    selected = null
                })
                AdminActionButton("Deshabilitar", tone = ActionTone.Danger, onClick = {
                    // TODO: deshabilitar la publicación/cuenta reportada
                    //  + insertar en acciones_administrativas
                    selected = null
                })
            }
        }
    }
}

private fun reportStatusTone(status: String) = when (status) {
    "Resuelto" -> BadgeTone.Success
    "En revisión" -> BadgeTone.Info
    else -> BadgeTone.Danger
}
