package com.pame.karsy.feature.admin

import androidx.annotation.StringRes
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

/** Mínimo de caracteres del motivo de una acción administrativa (la BD valida lo mismo). */
internal const val MIN_ADMIN_REASON = 10

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
    // Diálogo abierto: deshabilitar la publicación o descartar el reporte (ambos piden motivo).
    var dialog by rememberSaveable { mutableStateOf<ReportDecision?>(null) }
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
                    stringResource(R.string.admin2_review_keep),
                    AdminButtonStyle.Primary,
                    Modifier.weight(1f)
                ) {
                    vm.dismissMessage()
                    dialog = ReportDecision.Dismiss
                }
                AdminSheetButton(
                    stringResource(R.string.admin2_review_disable),
                    AdminButtonStyle.Destructive,
                    Modifier.weight(1f)
                ) {
                    vm.dismissMessage()
                    dialog = ReportDecision.Disable
                }
            }
        }
    }

    dialog?.let { decision ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message.takeIf { acted },
            texts = if (decision == ReportDecision.Disable) ReasonDialogTexts.Disable else ReasonDialogTexts.DismissReport,
            onConfirm = { reason ->
                acted = true
                vm.dismissMessage()
                val done = {
                    dialog = null
                    onDone()
                }
                if (decision == ReportDecision.Disable) vm.disableFromReport(report.id, reason, done)
                else vm.dismissReport(report.id, reason, done)
            },
            onDismiss = { if (!vm.working) dialog = null }
        )
    }
}

private enum class ReportDecision { Disable, Dismiss }

/** Textos del diálogo de motivo; por defecto, los de deshabilitar una publicación. */
data class ReasonDialogTexts(
    @StringRes val title: Int = R.string.admin2_disable_dialog_title,
    @StringRes val description: Int = R.string.admin2_disable_dialog_desc,
    @StringRes val hint: Int = R.string.admin2_disable_dialog_hint,
    @StringRes val confirm: Int = R.string.admin2_disable_confirm,
    @StringRes val working: Int = R.string.admin2_disable_dialog_working,
) {
    companion object {
        val Disable = ReasonDialogTexts()
        val RejectProposal = ReasonDialogTexts(
            R.string.admin2_reject_dialog_title, R.string.admin2_reject_dialog_desc,
            R.string.admin2_reject_dialog_hint, R.string.admin2_reject_confirm, R.string.admin2_reject_dialog_working,
        )
        val SuspendAccount = ReasonDialogTexts(
            R.string.admin2_suspend_dialog_title, R.string.admin2_suspend_dialog_desc,
            R.string.admin2_suspend_dialog_hint, R.string.admin2_suspend_confirm, R.string.admin2_suspend_dialog_working,
        )
        val SuspendLot = SuspendAccount.copy(title = R.string.admin2_suspend_lot_dialog_title)
        val DismissReport = ReasonDialogTexts(
            R.string.admin2_dismiss_dialog_title, R.string.admin2_dismiss_dialog_desc,
            R.string.admin2_dismiss_dialog_hint, R.string.admin2_dismiss_confirm, R.string.admin2_dismiss_dialog_working,
        )
        /** Reporte de cuenta que procede cuando la cuenta ya estaba suspendida. */
        val CloseReport = ReasonDialogTexts(
            R.string.admin2_close_report_dialog_title, R.string.admin2_close_report_dialog_desc,
            R.string.admin2_close_report_dialog_hint, R.string.admin2_close_report_confirm, R.string.admin2_close_report_dialog_working,
        )
    }
}

/** Pide el motivo obligatorio de una acción administrativa (rechazar, deshabilitar, suspender…). */
@Composable
fun AdminReasonDialog(
    working: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    texts: ReasonDialogTexts = ReasonDialogTexts.Disable,
) {
    var reason by rememberSaveable { mutableStateOf("") }
    val length = reason.trim().length
    val valid = length >= MIN_ADMIN_REASON

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsyWhite,
        title = {
            Text(
                stringResource(texts.title),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
        },
        text = {
            Column {
                Text(
                    stringResource(texts.description),
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
                        Text(stringResource(texts.hint), fontFamily = DmSans, fontSize = 13.sp)
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
                    else stringResource(R.string.admin2_disable_dialog_min, MIN_ADMIN_REASON, length),
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
                    stringResource(if (working) texts.working else texts.confirm),
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
