package com.pame.karsy.core.components

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyNavy
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
        IconButton(onClick = onBack) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.core_back),
                tint = KarsyInk,
                modifier = Modifier.size(28.dp)
            )
        }
        KarsyBrand(logoSize = 38.dp, fontSize = 20.sp, color = logoColor)
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
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, KarsyBorder, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = stringResource(R.string.core_back),
                        tint = KarsyInk,
                        modifier = Modifier
                            .size(16.dp)
                            .padding(start = 3.dp)
                    )
                }
            }
        }
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
                fontSize = 18.sp,
                color = KarsyInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        actions()
    }
}
