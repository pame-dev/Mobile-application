package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder

private enum class VehicleDecision { Approve, Reject, Disable, Enable }

/**
 * Acciones de admin al revisar una publicación desde Vehículos. Según el estado:
 * propuesta pendiente → aprobar / rechazar; activa → deshabilitar; deshabilitada por
 * admin → rehabilitar. En cualquier otro estado (pausada por el dueño, vendida,
 * rechazada) no hay acciones. [onDone] se llama solo cuando la BD aceptó la acción.
 */
@Composable
fun AdminVehicleReviewBar(vm: AdminViewModel, vehicle: AdminVehicle, onDone: () -> Unit) {
    val actions = when {
        vehicle.pendingProposalId != null -> listOf(VehicleDecision.Reject, VehicleDecision.Approve)
        vehicle.status == "Activo" && vehicle.enabledByAdmin -> listOf(VehicleDecision.Disable)
        vehicle.status == "Deshabilitado" -> listOf(VehicleDecision.Enable)
        else -> emptyList()
    }
    if (actions.isEmpty()) return

    // Rechazar y deshabilitar piden motivo; aprobar y rehabilitar no.
    var dialog by rememberSaveable { mutableStateOf<VehicleDecision?>(null) }
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
            // Edición corregida de una publicación deshabilitada: aprobarla la rehabilita (trigger en la BD).
            if (vehicle.pendingProposalId != null && !vehicle.enabledByAdmin) {
                Text(
                    stringResource(R.string.admin2_review_approve_reenables),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = BadgeTone.Warning.fg
                )
            }
            if (acted && dialog == null && !vm.working) vm.message?.let {
                Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                actions.forEach { decision ->
                    val (label, style) = when (decision) {
                        VehicleDecision.Approve -> R.string.admin2_approve to AdminButtonStyle.Positive
                        VehicleDecision.Reject -> R.string.admin2_reject to AdminButtonStyle.Destructive
                        VehicleDecision.Disable -> R.string.admin2_review_disable to AdminButtonStyle.Destructive
                        VehicleDecision.Enable -> R.string.admin2_review_enable to AdminButtonStyle.Positive
                    }
                    AdminSheetButton(stringResource(label), style, Modifier.weight(1f)) {
                        if (vm.working) return@AdminSheetButton
                        vm.dismissMessage()
                        when (decision) {
                            VehicleDecision.Approve -> {
                                acted = true
                                vm.moderate(vehicle, approve = true, onSuccess = onDone)
                            }
                            VehicleDecision.Enable -> {
                                acted = true
                                vm.setVehicleEnabled(vehicle, enabled = true, onSuccess = onDone)
                            }
                            else -> dialog = decision
                        }
                    }
                }
            }
        }
    }

    dialog?.let { decision ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message.takeIf { acted },
            texts = if (decision == VehicleDecision.Reject) ReasonDialogTexts.RejectProposal else ReasonDialogTexts.Disable,
            onConfirm = { reason ->
                acted = true
                vm.dismissMessage()
                val done = {
                    dialog = null
                    onDone()
                }
                if (decision == VehicleDecision.Reject) vm.moderate(vehicle, approve = false, reason = reason, onSuccess = done)
                else vm.setVehicleEnabled(vehicle, enabled = false, reason = reason, onSuccess = done)
            },
            onDismiss = { if (!vm.working) dialog = null }
        )
    }
}
