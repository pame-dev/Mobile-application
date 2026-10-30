package com.pame.karsy.core.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/**
 * Barra superior de los formularios: flecha atrás a la izquierda y logo centrado
 * (PhoneTopBar del mockup).
 */
@Composable
fun BackTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    logoColor: Color = KarsyInk,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 24.dp, top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        KarsyBackButton(onClick = onBack)
        KarsyBrand(logoSize = 38.dp, fontSize = KarsyTextSize.Headline, color = logoColor)
        Spacer(Modifier.width(30.dp))
    }
}

/**
 * Encabezado blanco con botón atrás y título (WebSubHeader / Header del mockup).
 * [actions] se dibuja a la derecha.
 */
@Composable
fun SubHeader(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    /** Ícono a la izquierda del título (p. ej. engrane en Configuración, corazón en Favoritos). */
    titleIcon: ImageVector? = null,
    titleIconTint: Color = KarsyInk,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(KarsySurface)
            .statusBarsPadding()
            .height(60.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) KarsyBackButton(onClick = onBack)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (titleIcon != null) {
                Icon(titleIcon, contentDescription = null, tint = titleIconTint, modifier = Modifier.size(22.dp))
            }
            Text(
                text = title,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = KarsyTextSize.TitleLarge,
                color = KarsyInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        actions()
    }
}
