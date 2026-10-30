package com.pame.karsy.core.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

private val DialogGray: Color get() = KarsyMid
private val SoftGray: Color get() = if (Tema.oscuro) Color(0xFF26313B) else Color(0xFFF4F7F9)

/**
 * Modal de confirmación sobre un fondo oscuro que cubre toda la pantalla:
 * ícono, título, mensaje y botones "Cancelar" (o [cancelText]) / [confirmText].
 * Debe ir al final de un Box a pantalla completa para quedar encima del contenido.
 */
@Composable
fun ConfirmDialog(
    visible: Boolean,
    icon: ImageVector,
    accent: Color,
    title: String,
    message: String,
    confirmText: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    /** Texto del botón secundario; por defecto "Cancelar". */
    cancelText: String? = null,
) {
    if (visible) BackHandler(onBack = onCancel)
    val haptic = LocalHapticFeedback.current

    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                // Tocar fuera de la tarjeta cancela.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onCancel
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .animateEnterExit(enter = scaleIn(initialScale = 0.9f), exit = scaleOut(targetScale = 0.9f))
                    .padding(horizontal = 28.dp)
                    .widthIn(max = 360.dp)
                    .background(KarsySurface, RoundedCornerShape(KarsyRadius.Lg))
                    // Evita que un toque dentro de la tarjeta la cierre.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .size(64.dp)
                        .background(SoftGray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(28.dp))
                }
                Text(
                    title,
                    fontFamily = Outfit,
                    fontSize = KarsyTextSize.Headline,
                    fontWeight = FontWeight.Bold,
                    color = KarsyInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
                Text(
                    message,
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.BodyLarge,
                    lineHeight = 20.sp,
                    color = DialogGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 22.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onCancel,
                        shape = RoundedCornerShape(KarsyRadius.Md),
                        colors = ButtonDefaults.buttonColors(containerColor = SoftGray, contentColor = KarsyInk),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                    ) {
                        Text(
                            cancelText ?: stringResource(R.string.detail_cancel),
                            fontFamily = DmSans,
                            fontSize = KarsyTextSize.BodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onConfirm()
                        },
                        shape = RoundedCornerShape(KarsyRadius.Md),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = KarsyWhite),
                        elevation = null,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                    ) {
                        Text(confirmText, fontFamily = Outfit, fontSize = KarsyTextSize.BodyLarge, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
