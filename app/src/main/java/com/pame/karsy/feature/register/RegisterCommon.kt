package com.pame.karsy.feature.register

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

/** Título grande + subtítulo gris que usan las pantallas de registro. */
@Composable
internal fun RegisterHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp)
    ) {
        Text(
            title,
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.02).em,
            color = KarsyNavy
        )
        Spacer(Modifier.height(6.dp))
        Text(subtitle, fontFamily = DmSans, fontSize = 15.sp, lineHeight = 21.sp, color = KarsyMid)
    }
}

/** Mensaje de error debajo del botón "Crear perfil". */
@Composable
internal fun RegisterError(message: String?) {
    if (message == null) return
    Text(
        message,
        fontFamily = DmSans,
        fontSize = 13.sp,
        color = KarsyError,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    )
}
