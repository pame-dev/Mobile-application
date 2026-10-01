package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

/**
 * Un reporte pendiente se revisa sobre la publicación misma ([onReviewReport]);
 * los ya resueltos abren su detalle con la resolución.
 */
@Composable
fun AdminReportsSection(vm: AdminViewModel, onCarClick: (Long) -> Unit, onReviewReport: (Long) -> Unit) {
    var status by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminReport?>(null) }

    val reports = vm.reports
    val filtered = reports.filter { status == "Todos" || it.status == status }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_section_reports),
            description = stringResource(R.string.admin2_reports_desc)
        )
        AdminCard {
            AdminFilterBar {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.admin2_incidents),
                            fontFamily = Outfit,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KarsyNavy
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            pluralStringResource(R.plurals.admin2_reports_count, reports.size, reports.size),
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            color = AdminColors.Muted
                        )
                    }
                    // Los estados vienen de la BD; solo se traduce lo que se muestra.
                    val statusValues = listOf("Todos", "Pendiente", "Atendido", "Descartado")
                    val statusLabels = statusValues.map { adminValueLabel(it) }
                    AdminSelect(
                        value = adminValueLabel(status),
                        options = statusLabels,
                        onSelect = { label -> status = statusValues[statusLabels.indexOf(label)] }
                    )
                }
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Outlined.Flag,
                    title = stringResource(R.string.admin2_reports_empty_title),
                    description = stringResource(R.string.admin2_reports_empty_desc)
                )
            } else {
                filtered.forEachIndexed { index, report ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminBadge(adminValueLabel(report.type), if (report.type == "Usuario") BadgeTone.Info else BadgeTone.Neutral)
                            Spacer(Modifier.weight(1f))
                            AdminStatusBadge(report.status)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdminThumbnail(report.imageUrl, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                report.target,
                                fontFamily = DmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField(stringResource(R.string.admin2_reported_by), report.reporter, Modifier.weight(1.3f))
                            AdminField(stringResource(R.string.admin2_date), report.date, Modifier.weight(1f))
                            AdminActionButton(stringResource(R.string.admin2_review), onClick = {
                                if (report.status == "Pendiente") onReviewReport(report.id) else selected = report
                            })
                        }
                    }
                }
            }
        }
    }

    selected?.let { report ->
        AdminDetailModal(
            title = stringResource(R.string.admin2_report_review_title),
            subtitle = stringResource(R.string.admin2_report_number, report.id.toString().padStart(4, '0')),
            onDismiss = { selected = null }
        ) {
            AdminDetailImage(report.imageUrl)
            DetailRow(stringResource(R.string.admin2_type), adminValueLabel(report.type))
            DetailRow(stringResource(R.string.admin2_reported_by), report.reporter)
            DetailRow(stringResource(R.string.admin2_value_publication), report.target)
            DetailRow(stringResource(R.string.admin2_reason), report.reason)
            DetailRow(stringResource(R.string.admin2_status), adminValueLabel(report.status))
            report.resolution?.let { DetailRow(stringResource(R.string.admin2_resolution), it) }
            // Solo un reporte pendiente se puede resolver (admin_resolver_reporte).
            AdminSheetActions {
                AdminSheetButton(stringResource(R.string.admin2_view_publication), AdminButtonStyle.Secondary) {
                    selected = null
                    onCarClick(report.publicationId)
                }
            }
        }
    }
}
