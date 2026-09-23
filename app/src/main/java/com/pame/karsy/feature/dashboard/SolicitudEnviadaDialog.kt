package com.pame.karsy.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Confirmación "Solicitud enviada" tras pedir destacar un vehículo. */
@Composable
fun SolicitudEnviadaDialog(carName: String, onClose: () -> Unit) {
    DashboardSheet(onDismiss = onClose) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(KarsyTealSoft),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(KarsyTeal),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(24.dp))
                }
            }
            Text(
                "Solicitud enviada",
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                buildAnnotatedString {
                    append("Tu solicitud para destacar ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = KarsyNavy)) { append(carName) }
                    append(" fue enviada correctamente.")
                },
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = KarsyCharcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "El equipo de Karsy la revisará pronto.",
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )
            SheetPrimaryButton(text = "Entendido", onClick = onClose, fontSize = 14)
        }
    }
}
