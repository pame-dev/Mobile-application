package com.pame.karsy.feature.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Hoja inferior "¿Quieres destacar tu vehículo?" (DestacarModal del mockup). */
@Composable
fun DestacarDialog(carName: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    DashboardSheet(onDismiss = onCancel) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono de estrella con halo
            Box(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .size(104.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(KarsyTeal.copy(alpha = 0.35f), KarsyTeal.copy(alpha = 0f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(KarsyTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(KarsyTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(32.dp))
                    }
                }
            }

            Text(
                "¿Quieres destacar tu vehículo?",
                fontFamily = Outfit,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                carName,
                fontFamily = DmSans,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(KarsyTealSoft)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Text(
                "Haz que tu publicación tenga mayor visibilidad y aparezca en una sección destacada de la plataforma.",
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 21.sp,
                color = KarsyCharcoal,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "Antes de destacarla, un administrador deberá revisar y aprobar tu publicación. Una vez aprobada, tu vehículo podrá aparecer como publicación destacada.",
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            HorizontalDivider(thickness = 1.dp, color = KarsyBg, modifier = Modifier.padding(bottom = 24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SheetPrimaryButton(text = "Solicitar destacar", onClick = onConfirm, fontSize = 15)
                TextButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancelar", fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KarsyMid)
                }
            }
        }
    }
}

/**
 * Contenedor común de las hojas del panel: fondo oscurecido que cierra al tocarlo y
 * tarjeta blanca redondeada anclada abajo con su "agarradera".
 */
@Composable
internal fun DashboardSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyNavy.copy(alpha = 0.52f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(28.dp), ambientColor = KarsyNavy, spotColor = KarsyNavy)
                .clip(RoundedCornerShape(28.dp))
                .background(KarsyWhite)
                // Evita que un toque dentro de la tarjeta la cierre.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(KarsyBorderMuted)
            )
            content()
        }
    }
}

@Composable
internal fun SheetPrimaryButton(text: String, onClick: () -> Unit, fontSize: Int) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
        contentPadding = PaddingValues(vertical = if (fontSize >= 15) 15.dp else 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, fontFamily = Outfit, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}
