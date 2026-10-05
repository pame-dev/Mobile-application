package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

/**
 * Revisión de un reporte de cuenta ("Usuario"): en lugar de la publicación se muestra
 * la cuenta del vendedor y sus publicaciones; abajo, "No suspender" / "Suspender cuenta".
 * [onDone] se llama solo cuando la BD aceptó la acción.
 */
@Composable
fun UserReportReviewScreen(
    vm: AdminViewModel,
    report: AdminReport,
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    onDone: () -> Unit,
) {
    val user = vm.users.firstOrNull { it.id == report.ownerId }
    val listings = vm.vehicles.filter { it.sellerId == report.ownerId }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(
                    Icons.Rounded.ChevronLeft,
                    contentDescription = stringResource(R.string.detail_back),
                    tint = KarsyNavy,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                stringResource(R.string.admin2_review_account_title),
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = KarsyNavy,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            ReportReviewNotice(report)

            Column(Modifier.padding(horizontal = 20.dp)) {
                if (user == null) {
                    Text(
                        stringResource(R.string.admin2_msg_owner_not_found),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = KarsyMid
                    )
                } else {
                    AdminCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            AdminUserRowContent(user)
                        }
                        HorizontalDivider(thickness = 1.dp, color = AdminColors.RowDivider)
                        Column(Modifier.padding(horizontal = 14.dp)) {
                            AdminUserDetailRows(user)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.admin2_account_listings, listings.size),
                    fontFamily = Outfit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy
                )
                Spacer(Modifier.height(10.dp))
                AdminCard {
                    if (listings.isEmpty()) {
                        Text(
                            stringResource(R.string.admin2_account_no_listings),
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            color = KarsyMid,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    listings.forEachIndexed { index, vehicle ->
                        ListingRow(
                            vehicle = vehicle,
                            reported = vehicle.id == report.publicationId,
                            onClick = { onCarClick(vehicle.id) }
                        )
                        if (index != listings.lastIndex) HorizontalDivider(thickness = 1.dp, color = AdminColors.RowDivider)
                    }
                }
            }
        }

        if (report.status == "Pendiente") {
            UserReportReviewBar(
                vm = vm,
                report = report,
                alreadySuspended = user?.status == "Suspendido",
                // El admin no puede suspenderse a sí mismo (la BD también lo impide).
                canSuspend = report.ownerId != null && report.ownerId != SessionManager.userId,
                onDone = onDone
            )
        }
    }
}

@Composable
private fun ListingRow(vehicle: AdminVehicle, reported: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        AdminThumbnail(vehicle.imageUrl, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                vehicle.name,
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text("${vehicle.year} · ${vehicle.price}", fontFamily = DmSans, fontSize = 12.sp, color = AdminColors.TableText)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (reported) AdminBadge(stringResource(R.string.admin2_badge_reported), BadgeTone.Danger)
            AdminStatusBadge(vehicle.status)
        }
    }
}

/** "No suspender" descarta el reporte; "Suspender cuenta" suspende al vendedor y atiende el reporte. */
@Composable
private fun UserReportReviewBar(
    vm: AdminViewModel,
    report: AdminReport,
    alreadySuspended: Boolean,
    canSuspend: Boolean,
    onDone: () -> Unit,
) {
    // true = suspender (o atender si ya estaba suspendida), false = descartar.
    var dialog by rememberSaveable { mutableStateOf<Boolean?>(null) }
    // Solo se muestra el error de una acción lanzada desde esta pantalla.
    var acted by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.background(KarsyBg)) {
        HorizontalDivider(color = KarsyBorder, thickness = 1.dp)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp)
        ) {
            if (acted && dialog == null && !vm.working) vm.message?.let {
                Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSheetButton(
                    stringResource(R.string.admin2_review_keep_account),
                    AdminButtonStyle.Primary,
                    Modifier.weight(1f)
                ) {
                    vm.dismissMessage()
                    dialog = false
                }
                if (canSuspend) {
                    AdminSheetButton(
                        stringResource(if (alreadySuspended) R.string.admin2_close_report_confirm else R.string.admin2_suspend_account),
                        AdminButtonStyle.Destructive,
                        Modifier.weight(1f)
                    ) {
                        vm.dismissMessage()
                        dialog = true
                    }
                }
            }
        }
    }

    dialog?.let { suspend ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message.takeIf { acted },
            texts = when {
                !suspend -> ReasonDialogTexts.DismissReport
                alreadySuspended -> ReasonDialogTexts.CloseReport
                else -> ReasonDialogTexts.SuspendAccount
            },
            onConfirm = { reason ->
                acted = true
                vm.dismissMessage()
                val done = {
                    dialog = null
                    onDone()
                }
                if (suspend) vm.suspendFromReport(report, reason, done)
                else vm.dismissReport(report.id, reason, done)
            },
            onDismiss = { if (!vm.working) dialog = null }
        )
    }
}
