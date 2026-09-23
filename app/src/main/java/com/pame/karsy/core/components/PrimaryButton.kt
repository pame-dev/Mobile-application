package com.pame.karsy.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyDisabled
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Botón principal azul marino, ancho completo (PrimaryButton del mockup). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = KarsyNavy,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = KarsyWhite,
            disabledContainerColor = KarsyDisabled,
            disabledContentColor = KarsyMid,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (enabled) 6.dp else 0.dp, RoundedCornerShape(14.dp), ambientColor = KarsyNavy, spotColor = KarsyNavy)
    ) {
        Text(text, fontFamily = Outfit, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Botón secundario con borde (Limpiar, Cancelar...). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = KarsyNavy,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, KarsyBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = KarsyWhite, contentColor = contentColor),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Text(text, fontFamily = Outfit, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
