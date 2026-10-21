package com.pame.karsy.feature.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.util.Constancia

/**
 * "Verificación de usuarios": documentos que enviaron los usuarios (lotes y particulares).
 * El admin abre el documento (URL firmada), lo aprueba o lo rechaza con un motivo.
 */
@Composable
fun AdminVerificationsSection(vm: AdminViewModel) {
    val context = LocalContext.current
    val verifications = vm.verifications
    var filter by rememberSaveable { mutableStateOf(VerificationStatus.Pendiente) }
    var rejectingId by rememberSaveable { mutableStateOf<String?>(null) }
    val visible = verifications.filter { it.status == filter }

    // Abre la URL firmada con la app que el teléfono tenga para verla (navegador o visor de PDF).
    val abrir: (UserVerification) -> Unit = { v ->
        vm.openDocument(v) { url ->
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: ActivityNotFoundException) {
                vm.message = texto(R.string.admin2_verif_open_error)
            }
        }
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_verif_title),
            description = stringResource(R.string.admin2_verif_desc)
        )

        AdminCard {
            AdminFilterBar {
                val statusLabels = VerificationStatus.entries.map { status ->
                    stringResource(R.string.admin1_filter_with_count, verificationFilterLabel(status), verifications.count { it.status == status })
                }
                AdminListHeader(
                    title = stringResource(R.string.admin2_verif_list_title),
                    count = pluralStringResource(R.plurals.admin2_verif_count, verifications.size, verifications.size)
                ) {
                    AdminSelect(
                        value = statusLabels[filter.ordinal],
                        options = statusLabels,
                        onSelect = { label -> statusLabels.indexOf(label).takeIf { it >= 0 }?.let { filter = VerificationStatus.entries[it] } }
                    )
                }
            }

            // Mientras carga: filas de esqueleto en lugar de "no hay documentos".
            if (vm.loading && visible.isEmpty()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    repeat(4) { SkeletonListRow(circle = false) }
                }
            } else if (visible.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Outlined.VerifiedUser,
                    title = stringResource(R.string.admin2_verif_empty),
                    description = stringResource(R.string.admin2_verif_desc)
                )
            } else {
                visible.forEachIndexed { index, verification ->
                    AdminListRow(isLast = index == visible.lastIndex) {
                        VerificationRow(
                            verification = verification,
                            onOpen = { abrir(verification) },
                            onApprove = { vm.resolveVerification(verification, approve = true) },
                            onReject = {
                                vm.dismissMessage()
                                rejectingId = verification.id
                            }
                        )
                    }
                }
            }
        }
    }

    verifications.firstOrNull { it.id == rejectingId }?.let { verification ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message,
            texts = ReasonDialogTexts.RejectVerification,
            onConfirm = { reason ->
                vm.resolveVerification(verification, approve = false, reason = reason) { rejectingId = null }
            },
            onDismiss = { if (!vm.working) rejectingId = null }
        )
    }
}

@Composable
private fun verificationFilterLabel(status: VerificationStatus): String = when (status) {
    VerificationStatus.Pendiente -> stringResource(R.string.admin2_verif_filter_pending)
    VerificationStatus.Verificado -> stringResource(R.string.admin2_verif_filter_verified)
    VerificationStatus.Rechazado -> stringResource(R.string.admin2_verif_filter_rejected)
}

/** "Lote" / "Particular"; un tipo desconocido se muestra tal cual. */
@Composable
private fun userTypeLabel(type: String): String = when (type) {
    "lote" -> stringResource(R.string.admin2_value_lot)
    "particular" -> stringResource(R.string.admin2_value_individual)
    else -> type
}

/** Nombre del documento; un tipo que la app aún no conoce se muestra tal cual. */
@Composable
private fun documentTypeLabel(type: String): String = when (type) {
    Constancia.TIPO_DOCUMENTO -> stringResource(R.string.admin2_verif_doc_constancia)
    else -> type
}

/** Fila con el formato de Destacados: tiempo y estado, usuario y documento, y las acciones. */
@Composable
private fun VerificationRow(
    verification: UserVerification,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (verification.sentAgo.isNotEmpty()) AdminBadge(verification.sentAgo, BadgeTone.Neutral)
        Spacer(Modifier.weight(1f))
        when (verification.status) {
            VerificationStatus.Pendiente -> AdminBadge(stringResource(R.string.admin2_verif_status_pending), BadgeTone.Warning)
            VerificationStatus.Verificado -> AdminBadge(stringResource(R.string.admin2_verif_status_verified), BadgeTone.Success)
            VerificationStatus.Rechazado -> AdminBadge(stringResource(R.string.admin2_verif_status_rejected), BadgeTone.Danger)
        }
    }
    Column {
        Text(verification.name, fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
        if (verification.email.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(verification.email, fontFamily = DmSans, fontSize = 12.sp, color = AdminColors.TableText)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AdminField(stringResource(R.string.admin2_verif_user_type), userTypeLabel(verification.userType), Modifier.weight(1f))
        AdminField(stringResource(R.string.admin2_verif_document), documentTypeLabel(verification.documentType), Modifier.weight(1f))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AdminField(stringResource(R.string.admin2_verif_sent_on), verification.sentOn, Modifier.weight(1f))
        val reviewedOn = verification.reviewedOn
        if (reviewedOn != null) {
            AdminField(stringResource(R.string.admin2_verif_reviewed_on), reviewedOn, Modifier.weight(1f))
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
    if (verification.status != VerificationStatus.Pendiente && !verification.reviewedBy.isNullOrBlank()) {
        AdminField(stringResource(R.string.admin2_verif_reviewed_by), verification.reviewedBy)
    }
    if (verification.status == VerificationStatus.Rechazado && !verification.rejectionReason.isNullOrBlank()) {
        AdminField(stringResource(R.string.admin2_verif_reason_label), verification.rejectionReason)
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        AdminActionButton(stringResource(R.string.admin2_verif_open), onClick = onOpen)
        Spacer(Modifier.weight(1f))
        if (verification.status == VerificationStatus.Pendiente) {
            AdminActionButton(stringResource(R.string.admin1_reject), tone = ActionTone.Danger, onClick = onReject)
            AdminActionButton(stringResource(R.string.admin2_approve), tone = ActionTone.Success, onClick = onApprove)
        }
    }
}
