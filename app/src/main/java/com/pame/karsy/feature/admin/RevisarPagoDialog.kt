package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.copiarAlPortapapeles

/**
 * Revisar el pago de un destacado antes de aprobarlo (igual que "Revisar en REPUVE"): el
 * comprobante, el monto y la referencia esperados para buscarlos en la banca en línea, una guía
 * y tres casillas que activan Aprobar. La imagen se puede alterar: lo que cuenta es que el
 * dinero ya esté en la cuenta. Rechazar pide el motivo (como siempre).
 */
@Composable
internal fun RevisarPagoDialog(
    request: FeaturedRequest,
    working: Boolean,
    error: String?,
    onOpenReceipt: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var monto by rememberSaveable { mutableStateOf(false) }
    var encontrado by rememberSaveable { mutableStateOf(false) }
    var recibido by rememberSaveable { mutableStateOf(false) }
    val listo = !working && monto && encontrado && recibido
    val copiado = stringResource(R.string.admin2_repuve_copied)
    val amount = request.amount.orEmpty()
    val reference = request.reference.orEmpty()
    val vehiculo = if (request.year > 0) "${request.vehicle} ${request.year}" else request.vehicle

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsySurface,
        title = {
            Text(
                stringResource(R.string.admin2_pay_review_title, vehiculo),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.admin2_pay_review_sub), fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.NeutralText)
                // Se abre con el visor o navegador del teléfono (URL firmada de 10 min).
                AdminActionButton(stringResource(R.string.admin2_pay_view_receipt), tone = ActionTone.Success, onClick = onOpenReceipt)

                DatoParaCopiar(stringResource(R.string.admin2_pay_expected_amount), amount) {
                    copiarAlPortapapeles(context, it, amount, copiado)
                }
                DatoParaCopiar(stringResource(R.string.admin2_pay_expected_reference), reference) {
                    copiarAlPortapapeles(context, it, reference, copiado)
                }
                AdminField(stringResource(R.string.admin2_pay_lot), request.user)
                AdminField(stringResource(R.string.admin2_pay_vehicle), vehiculo)

                Text(
                    stringResource(R.string.admin2_pay_steps, amount, reference),
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
                CasillaRevision(monto, stringResource(R.string.admin2_pay_check_amount)) { monto = it }
                CasillaRevision(encontrado, stringResource(R.string.admin2_pay_check_found)) { encontrado = it }
                CasillaRevision(recibido, stringResource(R.string.admin2_pay_check_received)) { recibido = it }
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

/** Etiqueta, valor en monoespaciada (seleccionable) y botón Copiar; [onCopy] recibe la etiqueta. */
@Composable
private fun DatoParaCopiar(etiqueta: String, valor: String, onCopy: (String) -> Unit) {
    Column {
        Text(etiqueta, fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectionContainer(Modifier.weight(1f)) {
                Text(valor, fontFamily = FontFamily.Monospace, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
            }
            AdminActionButton(stringResource(R.string.admin2_curp_copy)) { onCopy(etiqueta) }
        }
    }
}
