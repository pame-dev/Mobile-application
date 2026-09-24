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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.HeartToggle
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car

/** Tarjeta de la cuadrícula "Todos los vehículos" (VehicleCard del mockup). */
@Composable
fun VehicleCard(
    car: Car,
    isFavorite: Boolean,
    showHeart: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.08f), spotColor = KarsyNavy.copy(alpha = 0.14f))
            .clip(shape)
            .background(KarsyWhite)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(ImagePlaceholder)
        ) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            if (showHeart) {
                HeartToggle(
                    filled = isFavorite,
                    onToggle = onToggleFavorite,
                    size = 32.dp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        car.title,
                        fontFamily = Outfit,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyNavy,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${car.year} · ${car.kilometraje}",
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        color = KarsyMid,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        car.price,
                        fontFamily = Outfit,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyTeal
                    )
                    Text(car.currency, fontFamily = DmSans, fontSize = 10.sp, color = KarsyMid)
                }
            }
            Text(
                car.description,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = KarsyTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
