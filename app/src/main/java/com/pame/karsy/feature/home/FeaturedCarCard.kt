package com.pame.karsy.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.HeartToggle
import com.pame.karsy.core.components.StarBadge
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car

/** Fondo gris azulado mientras carga la foto. */
internal val ImagePlaceholder = Color(0xFFDDE6EC)

/** Tarjeta grande del carrusel "Vehículos destacados" (FeaturedCard del mockup). */
@Composable
fun FeaturedCarCard(
    car: Car,
    isFavorite: Boolean,
    showHeart: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 300.dp,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .width(width)
            .shadow(10.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.10f), spotColor = KarsyNavy.copy(alpha = 0.18f))
            .clip(shape)
            .background(KarsyWhite)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(ImagePlaceholder)
        ) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            car.badge?.let {
                StarBadge(it, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
            }
            if (showHeart) {
                HeartToggle(
                    filled = isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 12.dp)
                )
            }
        }
        Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        car.title,
                        fontFamily = Outfit,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyNavy,
                        lineHeight = 20.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        car.year.toString(),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = KarsyMid,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        append(car.price)
                        append(" ")
                        withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, color = KarsyMid)) {
                            append(car.currency)
                        }
                    },
                    fontFamily = Outfit,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyTeal,
                    maxLines = 1
                )
            }
            Text(
                car.description,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 19.5.sp,
                color = KarsyTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
