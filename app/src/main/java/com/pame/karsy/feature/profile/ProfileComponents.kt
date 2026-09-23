package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCheckBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Fondo gris claro de las imágenes mientras cargan (#dde6ec del mockup). */
internal val ImagePlaceholder = Color(0xFFDDE6EC)

/** Sombra suave usada en las tarjetas del mockup (0 2px 12px rgba(13,43,69,0.07)). */
internal val CardShadow = KarsyNavy.copy(alpha = 0.18f)

/** Tarjeta blanca redondeada con borde (#E2E8ED) y sombra ligera. */
@Composable
internal fun ProfileCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    borderColor: Color = KarsyBorder,
    elevation: Dp = 3.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation, shape, ambientColor = CardShadow, spotColor = CardShadow)
            .clip(shape)
            .background(KarsyWhite)
            .border(1.dp, borderColor, shape),
        content = content
    )
}

/** Separador entre filas de una lista dentro de una tarjeta (1px #F4F7F9). */
@Composable
internal fun RowDivider() {
    HorizontalDivider(thickness = 1.dp, color = KarsyBg)
}

/** Flecha gris a la derecha de las filas navegables. */
@Composable
internal fun RowChevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = KarsyCheckBorder,
        modifier = Modifier.size(22.dp)
    )
}

/** Degradado navy → teal de la portada del perfil. */
internal val ProfileCoverBrush = Brush.linearGradient(listOf(KarsyNavy, KarsyTeal))

/** Iniciales para el avatar cuando la cuenta no tiene foto. */
internal fun initialsOf(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }

/**
 * Avatar circular: foto si existe, si no las iniciales sobre el degradado de la marca.
 */
@Composable
internal fun ProfileAvatar(
    name: String,
    avatarUrl: String?,
    size: Dp,
    initialsSize: TextUnit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ProfileCoverBrush),
        contentAlignment = Alignment.Center
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initialsOf(name),
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = initialsSize,
                color = KarsyWhite
            )
        }
    }
}
