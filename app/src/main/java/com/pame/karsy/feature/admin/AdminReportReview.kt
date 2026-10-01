package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Mínimo de caracteres del comentario al deshabilitar (la BD valida lo mismo). */
internal const val MIN_DISABLE_REASON = 10

/** Datos del reporte arriba de la publicación que se está revisando. */
@Composable
fun ReportReviewNotice(report: AdminReport) {
    Column(
        Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BadgeTone.Danger.bg)
            .border(1.dp, BadgeTone.Danger.fg.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Flag, contentDescription = null, tint = BadgeTone.Danger.fg, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.admin2_report_number, report.id.toString().padStart(4, '0')),
                fontFamily = Outfit,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = BadgeTone.Danger.fg,
                modifier = Modifier.weight(1f)
            )
            Text(report.date, fontFamily = DmSans, fontSize = 11.sp, color = AdminColors.NeutralText)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "${stringResource(R.string.admin2_reported_by)}: ${report.reporter}",
            fontFamily = DmSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "${stringResource(R.string.admin2_reason)}: ${report.reason}",
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = KarsyCharcoal
        )
    }
}

/**
 * Barra inferior al revisar un reporte: "No deshabilitar" descarta el reporte y
 * "Deshabilitar publicación" pide el comentario que verá el dueño.
 * [onDone] se llama solo cuando la BD aceptó la acción.
 */
@Composable
fun ReportReviewBar(vm: AdminViewModel, report: AdminReport, onDone: () -> Unit) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
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
            if (acted && !showDialog && !vm.working) vm.message?.let {
                Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminSheetButton(
                    stringResource(R.string.admin2_review_keep),
                    AdminButtonStyle.Primary,
                    Modifier.weight(1f)
                ) {
                    if (vm.working) return@AdminSheetButton
                    acted = true
                    vm.dismissMessage()
                    vm.dismissReport(report.id, onDone)
                }
                AdminSheetButton(
                    stringResource(R.string.admin2_review_disable),
                    AdminButtonStyle.Destructive,
                    Modifier.weight(1f)
                ) {
                    vm.dismissMessage()
                    showDialog = true
                }
            }
        }
    }

    if (showDialog) {
        DisableReasonDialog(
            working = vm.working,
            error = vm.message.takeIf { acted },
            onConfirm = { reason ->
                acted = true
                vm.dismissMessage()
                vm.disableFromReport(report.id, reason) {
                    showDialog = false
                    onDone()
                }
            },
            onDismiss = { if (!vm.working) showDialog = false }
        )
    }
}

/** Pide el comentario obligatorio para deshabilitar una publicación. */
@Composable
fun DisableReasonDialog(
    working: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    val length = reason.trim().length
    val valid = length >= MIN_DISABLE_REASON

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsyWhite,
        title = {
            Text(
                stringResource(R.string.admin2_disable_dialog_title),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
        },
        text = {
            Column {
                Text(
                    stringResource(R.string.admin2_disable_dialog_desc),
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = AdminColors.NeutralText
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { if (it.length <= 500) reason = it },
                    placeholder = {
                        Text(stringResource(R.string.admin2_disable_dialog_hint), fontFamily = DmSans, fontSize = 13.sp)
                    },
                    textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = KarsyCharcoal),
                    enabled = !working,
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KarsyNavy,
                        unfocusedBorderColor = KarsyBorder,
                    ),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (valid) stringResource(R.string.admin2_disable_dialog_count, length)
                    else stringResource(R.string.admin2_disable_dialog_min, MIN_DISABLE_REASON, length),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    color = if (valid) AdminColors.Muted else BadgeTone.Warning.fg
                )
                error?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim()) }, enabled = valid && !working) {
                Text(
                    stringResource(if (working) R.string.admin2_disable_dialog_working else R.string.admin2_disable_confirm),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    color = if (valid && !working) BadgeTone.Danger.fg else AdminColors.Muted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) {
                Text(stringResource(R.string.admin1_cancel), fontFamily = DmSans, color = KarsyNavy)
            }
        }
    )
}
