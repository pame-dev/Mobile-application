package com.pame.karsy.core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

/** Ícono del logo con esquinas redondeadas (35 % del tamaño, como en el mockup). */
@Composable
fun KarsyLogo(size: Dp = 34.dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.karsy_logo),
        contentDescription = "Karsy",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(maxOf(4f, size.value * 0.35f).dp))
    )
}

/** Logo + palabra "Karsy". */
@Composable
fun KarsyBrand(
    modifier: Modifier = Modifier,
    logoSize: Dp = 34.dp,
    fontSize: TextUnit = 19.sp,
    color: Color = KarsyNavy,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        KarsyLogo(size = logoSize)
        Text(
            text = "Karsy",
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = color,
            letterSpacing = 0.03.em
        )
    }
}
