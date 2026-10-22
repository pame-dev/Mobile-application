package com.pame.karsy.feature.admin

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pame.karsy.R
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Constancia
import com.pame.karsy.core.util.Curp
import java.text.DateFormat
import java.util.GregorianCalendar

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
    var reviewingCurpId by rememberSaveable { mutableStateOf<String?>(null) }
    var reviewingConstanciaId by rememberSaveable { mutableStateOf<String?>(null) }
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
                            onReviewCurp = { reviewingCurpId = verification.id },
                            onReviewConstancia = {
                                vm.dismissMessage()
                                reviewingConstanciaId = verification.id
                            }
                        )
                    }
                }
            }
        }
    }

    verifications.firstOrNull { it.id == reviewingCurpId }?.let { verification ->
        RevisarCurpDialog(
            verification = verification,
            working = vm.working,
            // Si falla, el error se ve dentro de la ventana (no detrás).
            error = vm.message,
            onApprove = {
                vm.dismissMessage()
                vm.resolveVerification(verification, approve = true) { reviewingCurpId = null }
            },
            onReject = {
                reviewingCurpId = null
                vm.dismissMessage()
                rejectingId = verification.id
            },
            onDismiss = { if (!vm.working) reviewingCurpId = null }
        )
    }

    verifications.firstOrNull { it.id == reviewingConstanciaId }?.let { verification ->
        RevisarConstanciaDialog(
            verification = verification,
            working = vm.working,
            // Si falla, el error se ve dentro de la ventana (no detrás).
            error = vm.message,
            onOpen = { abrir(verification) },
            onApprove = {
                vm.dismissMessage()
                vm.resolveVerification(verification, approve = true) { reviewingConstanciaId = null }
            },
            onReject = {
                reviewingConstanciaId = null
                vm.dismissMessage()
                rejectingId = verification.id
            },
            onDismiss = { if (!vm.working) reviewingConstanciaId = null }
        )
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
    TIPO_CURP -> "CURP"
    else -> type
}

/** Fila con el formato de Destacados: tiempo y estado, usuario y documento, y las acciones. */
@Composable
private fun VerificationRow(
    verification: UserVerification,
    onOpen: () -> Unit,
    onReviewCurp: () -> Unit,
    onReviewConstancia: () -> Unit,
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
    // La CURP no tiene archivo: se muestra para compararla con el nombre del usuario.
    if (!verification.curp.isNullOrBlank()) {
        AdminField("CURP", verification.curp)
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
        if (verification.documentPath != null) AdminActionButton(stringResource(R.string.admin2_verif_open), onClick = onOpen)
        Spacer(Modifier.weight(1f))
        // CURP: se revisa en una ventana con la guía para comparar en gob.mx.
        if (verification.status == VerificationStatus.Pendiente && verification.documentType == TIPO_CURP) {
            AdminActionButton(stringResource(R.string.admin2_curp_review), tone = ActionTone.Success, onClick = onReviewCurp)
        } else if (verification.status == VerificationStatus.Pendiente) {
            // Constancia (y cualquier otro documento con archivo): ventana con la guía de qué revisar.
            AdminActionButton(stringResource(R.string.admin2_csf_review), tone = ActionTone.Success, onClick = onReviewConstancia)
        }
    }
}

/** Página oficial para consultar una CURP. */
private const val GOB_CURP = "https://www.gob.mx/curp/"

/** tipo_documento de la CURP de un particular (no tiene archivo). */
private const val TIPO_CURP = "curp"

/**
 * Revisar la CURP de un particular: pasos para consultarla en gob.mx, la CURP para copiar,
 * el nombre del perfil y los datos que trae la CURP para comparar. Aprobar se activa hasta
 * marcar las dos casillas; Rechazar pide el motivo (como en los demás documentos).
 */
@Composable
private fun RevisarCurpDialog(
    verification: UserVerification,
    working: Boolean,
    error: String?,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val curp = verification.curp.orEmpty()
    val datos = Curp.leer(curp)
    var existe by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf(false) }
    val copiada = stringResource(R.string.admin2_curp_copied)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsySurface,
        title = {
            Text(
                stringResource(R.string.admin2_curp_review_title, verification.name),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.admin2_curp_review_sub), fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.NeutralText)
                Text(
                    stringResource(R.string.admin2_curp_steps),
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = KarsyInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KarsyTealSoft)
                        .padding(12.dp)
                )
                SelectionContainer {
                    Text(curp, fontFamily = FontFamily.Monospace, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (curp.isNotBlank()) AdminActionButton(stringResource(R.string.admin2_curp_copy)) {
                        // Dato personal: marcado como sensible para que Android no lo muestre en la
                        // vista previa del portapapeles. Desde Android 13 el sistema ya avisa que se copió.
                        val clip = ClipData.newPlainText("CURP", curp).apply {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                            }
                        }
                        context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, copiada, Toast.LENGTH_SHORT).show()
                    }
                    AdminActionButton(stringResource(R.string.admin2_curp_open_gob), tone = ActionTone.Success) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, GOB_CURP.toUri()))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, GOB_CURP, Toast.LENGTH_LONG).show()
                        }
                    }
                }
                AdminField(stringResource(R.string.admin2_curp_profile_name), verification.name)
                Text(stringResource(R.string.admin2_curp_from_curp), fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted)
                if (datos != null) {
                    val fecha = DateFormat.getDateInstance(DateFormat.LONG, LocalConfiguration.current.locales[0])
                        .format(GregorianCalendar(datos.anio, datos.mes - 1, datos.dia).time)
                    AdminField(stringResource(R.string.admin2_curp_birth), fecha)
                    AdminField(
                        stringResource(R.string.admin2_curp_sex),
                        stringResource(
                            when (datos.sexo) {
                                'H' -> R.string.admin2_curp_sex_h
                                'M' -> R.string.admin2_curp_sex_m
                                else -> R.string.admin2_curp_sex_x
                            }
                        )
                    )
                    AdminField(
                        stringResource(R.string.admin2_curp_birth_state),
                        if (datos.estado == "NE") stringResource(R.string.admin2_curp_abroad) else Curp.ESTADOS.getValue(datos.estado)
                    )
                } else {
                    Text(stringResource(R.string.admin2_curp_unreadable), fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.NeutralText)
                }
                CasillaRevision(existe, stringResource(R.string.admin2_curp_check_exists)) { existe = it }
                CasillaRevision(nombre, stringResource(R.string.admin2_curp_check_name)) { nombre = it }
                error?.let { Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg) }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove, enabled = existe && nombre && !working) {
                Text(
                    stringResource(R.string.admin2_approve),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    color = if (existe && nombre && !working) BadgeTone.Success.fg else AdminColors.Muted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onReject, enabled = !working) {
                Text(stringResource(R.string.admin1_reject), fontFamily = DmSans, fontWeight = FontWeight.Bold, color = BadgeTone.Danger.fg)
            }
        }
    )
}

/**
 * Revisar la constancia de situación fiscal de un lote: guía de qué revisar, botón para ver
 * el documento (foto o PDF) y el nombre del lote para comparar. Aprobar se activa hasta marcar
 * las dos casillas; Rechazar pide el motivo (como en la CURP).
 */
@Composable
private fun RevisarConstanciaDialog(
    verification: UserVerification,
    working: Boolean,
    error: String?,
    onOpen: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    var documento by rememberSaveable { mutableStateOf(false) }
    var nombre by rememberSaveable { mutableStateOf(false) }
    val listo = documento && nombre && !working
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsySurface,
        title = {
            Text(
                stringResource(R.string.admin2_csf_review_title, verification.name),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.admin2_csf_review_sub), fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.NeutralText)
                Text(
                    stringResource(R.string.admin2_csf_steps),
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = KarsyInk,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KarsyTealSoft)
                        .padding(12.dp)
                )
                AdminField(stringResource(R.string.admin2_csf_lot_name), verification.name)
                if (verification.documentPath != null) {
                    AdminActionButton(stringResource(R.string.admin2_verif_open), tone = ActionTone.Success, onClick = onOpen)
                }
                CasillaRevision(documento, stringResource(R.string.admin2_csf_check_document)) { documento = it }
                CasillaRevision(nombre, stringResource(R.string.admin2_csf_check_name)) { nombre = it }
                error?.let { Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg) }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove, enabled = listo) {
                Text(
                    stringResource(R.string.admin2_approve),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    color = if (listo) BadgeTone.Success.fg else AdminColors.Muted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onReject, enabled = !working) {
                Text(stringResource(R.string.admin1_reject), fontFamily = DmSans, fontWeight = FontWeight.Bold, color = BadgeTone.Danger.fg)
            }
        }
    )
}

/** Casilla con texto; toda la fila se puede tocar. */
@Composable
internal fun CasillaRevision(marcada: Boolean, texto: String, onCambio: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = marcada, role = Role.Checkbox, onValueChange = onCambio)
    ) {
        Checkbox(checked = marcada, onCheckedChange = null)
        Text(texto, fontFamily = DmSans, fontSize = 13.sp, color = KarsyInk, modifier = Modifier.padding(start = 6.dp))
    }
}
