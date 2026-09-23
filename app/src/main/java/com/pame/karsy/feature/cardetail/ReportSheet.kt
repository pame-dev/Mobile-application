package com.pame.karsy.feature.cardetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.karsyTextFieldColors
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

// Tonos del flujo de reporte tomados del mockup.
private val ReportRed = Color(0xFFE53935)
private val ReportRedLight = Color(0xFFFDEDEC)
private val ReportDisabled = Color(0xFFD6DDE1)
private val RadioBorder = Color(0xFFB7C0C7)

private enum class ReportStep { TYPE, REASONS, SUCCESS }

private const val TYPE_PUBLICACION = "publicacion"
private const val TYPE_CUENTA = "cuenta"

private val publicacionReasons = listOf(
    "Información engañosa o falsa" to "Precio, kilometraje o datos incorrectos.",
    "Sospecha de fraude o estafa" to "",
    "Vehículo ya vendido o no disponible" to "",
    "Fotos de mala calidad, explícitas o robadas" to "",
    "Publicación duplicada" to "",
    "Otra razón" to "",
)

private val cuentaReasons = listOf(
    "Vendedor sospechoso o posible estafador" to "",
    "Suplantación de identidad" to "",
    "Lenguaje ofensivo o acoso en mensajes" to "",
    "Incumplimiento de tratos o spam" to "",
    "Otra razón" to "",
)

/** Flujo de reporte en varios pasos: tipo -> motivos -> enviado. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportSheet(onDismiss: () -> Unit) {
    var step by rememberSaveable { mutableStateOf(ReportStep.TYPE) }
    var reportType by rememberSaveable { mutableStateOf<String?>(null) }
    var reason by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = KarsyWhite,
        scrimColor = SheetScrim,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 28.dp)
        ) {
            when (step) {
                ReportStep.TYPE -> TypeStep(
                    onClose = onDismiss,
                    onChoose = { type ->
                        reportType = type
                        reason = ""
                        details = ""
                        step = ReportStep.REASONS
                    }
                )

                ReportStep.REASONS -> ReasonsStep(
                    isPublicacion = reportType == TYPE_PUBLICACION,
                    reason = reason,
                    details = details,
                    onReason = { reason = it },
                    onDetails = { details = it },
                    onBack = { step = ReportStep.TYPE },
                    onClose = onDismiss,
                    onSend = {
                        if (reason.isNotEmpty()) {
                            // TODO: insertar en public.reportes (id_publicacion o id_cuenta_reportada,
                            //  id_reportante, motivo_reporte = reason, detalles = details).
                            step = ReportStep.SUCCESS
                        }
                    }
                )

                ReportStep.SUCCESS -> SuccessStep(onClose = onDismiss)
            }
        }
    }
}

@Composable
private fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier, filled: Boolean = true) {
    IconButton(
        onClick = onClick,
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = if (filled) KarsyBg else Color.Transparent,
            contentColor = KarsyMid
        ),
        modifier = modifier.size(32.dp)
    ) {
        Icon(Icons.Rounded.Close, contentDescription = "Cerrar", modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TypeStep(onClose: () -> Unit, onChoose: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp)
    ) {
        Text(
            "¿Qué deseas reportar?",
            textAlign = TextAlign.Center,
            fontFamily = Outfit,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyNavy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 34.dp)
                .align(Alignment.Center)
        )
        CloseButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterEnd))
    }

    listOf(
        Triple(TYPE_PUBLICACION, "🚘⚠️", "Reportar publicación") to
            "Información falsa, fotos inapropiadas o posible fraude en este vehículo.",
        Triple(TYPE_CUENTA, "👤⚠️", "Reportar cuenta") to
            "Comportamiento sospechoso, suplantación o acoso por parte del vendedor.",
    ).forEach { (info, desc) ->
        val (type, icon, title) = info
        val shape = RoundedCornerShape(16.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .shadow(3.dp, shape, ambientColor = KarsyNavy, spotColor = KarsyNavy.copy(alpha = 0.25f))
                .clip(shape)
                .background(KarsyWhite)
                .border(1.dp, KarsyBorder, shape)
                .clickable { onChoose(type) }
                .padding(horizontal = 14.dp, vertical = 15.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ReportRedLight),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 14.sp)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyCharcoal,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
                Text(desc, fontFamily = DmSans, fontSize = 11.5.sp, lineHeight = 16.sp, color = KarsyMid)
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = KarsyMid,
                modifier = Modifier.size(22.dp)
            )
        }
    }

    Text(
        "Cancelar",
        textAlign = TextAlign.Center,
        fontFamily = DmSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = KarsyMid,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClose)
            .padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ReasonsStep(
    isPublicacion: Boolean,
    reason: String,
    details: String,
    onReason: (String) -> Unit,
    onDetails: (String) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onSend: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        IconButton(
            onClick = onBack,
            colors = IconButtonDefaults.iconButtonColors(containerColor = KarsyBg, contentColor = KarsyNavy),
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Regresar", modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (isPublicacion) "Razones para reportar esta publicación" else "Razones para reportar al vendedor",
                fontFamily = Outfit,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
            Text(
                "Selecciona el motivo que mejor describa el problema.",
                fontFamily = DmSans,
                fontSize = 11.5.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        CloseButton(onClick = onClose, filled = false)
    }

    (if (isPublicacion) publicacionReasons else cuentaReasons).forEach { (r, desc) ->
        val selected = reason == r
        val shape = RoundedCornerShape(13.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(shape)
                .background(if (selected) ReportRedLight else KarsyBg)
                .border(if (selected) 1.5.dp else 1.dp, if (selected) ReportRed else KarsyBorder, shape)
                .clickable { onReason(r) }
                .padding(horizontal = 13.dp, vertical = 12.dp)
        ) {
            Box(
                Modifier
                    .padding(top = 1.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(KarsyWhite)
                    .border(if (selected) 5.dp else 2.dp, if (selected) ReportRed else RadioBorder, CircleShape)
            )
            Column {
                Text(r, fontFamily = DmSans, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = KarsyCharcoal)
                if (desc.isNotEmpty()) {
                    Text(
                        desc,
                        fontFamily = DmSans,
                        fontSize = 10.5.sp,
                        color = KarsyMid,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }

    OutlinedTextField(
        value = details,
        onValueChange = onDetails,
        placeholder = {
            Text(
                "Escribe detalles adicionales que ayuden a la revisión (opcional)...",
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid
            )
        },
        minLines = 3,
        maxLines = 5,
        textStyle = TextStyle(fontFamily = DmSans, fontSize = 12.sp, lineHeight = 17.sp, color = KarsyCharcoal),
        shape = RoundedCornerShape(13.dp),
        colors = karsyTextFieldColors(container = KarsyBg),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    )

    val enabled = reason.isNotEmpty()
    Button(
        onClick = onSend,
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ReportRed,
            contentColor = KarsyWhite,
            disabledContainerColor = ReportDisabled,
            disabledContentColor = KarsyWhite
        ),
        contentPadding = PaddingValues(vertical = 14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .shadow(if (enabled) 5.dp else 0.dp, RoundedCornerShape(999.dp), spotColor = ReportRed, ambientColor = ReportRed)
    ) {
        Text("Enviar reporte", fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SuccessStep(onClose: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 2.dp)
    ) {
        Box(
            Modifier
                .padding(bottom = 14.dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(KarsyTealLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(32.dp))
        }
        Text(
            "Reporte enviado",
            fontFamily = Outfit,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyNavy,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            "Gracias por tu reporte. Nuestro equipo de administración revisará la información a la brevedad.",
            textAlign = TextAlign.Center,
            fontFamily = DmSans,
            fontSize = 12.5.sp,
            lineHeight = 19.sp,
            color = KarsyCharcoal,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .padding(bottom = 18.dp)
        )
        Button(
            onClick = onClose,
            shape = RoundedCornerShape(999.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Entendido", fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
