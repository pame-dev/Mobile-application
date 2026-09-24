package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.CarRepository

/**
 * Historial de autos vistos (WebHistorialView del mockup).
 * Sale de interacciones_publicacion tipo 'ver_detalle' de la cuenta en sesión.
 */
@Composable
fun HistoryScreen(onBack: () -> Unit, onCarClick: (Long) -> Unit) {
    val historyItems by produceState<List<Pair<Car, String>>?>(initialValue = null) {
        value = safeCall { CarRepository.history() }.getOrDefault(emptyList())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = "🕘 Historial",
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
        ) {
            val items = historyItems
            if (items == null) {
                CircularProgressIndicator(color = KarsyTeal, modifier = Modifier.align(Alignment.CenterHorizontally))
                return@Column
            }
            ProfileCard {
                if (items.isEmpty()) {
                    Text(
                        "Aún no has visto ningún vehículo.",
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        color = KarsyMid,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                items.forEachIndexed { idx, (car, sub) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarClick(car.id) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = car.imageUrl,
                            contentDescription = car.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ImagePlaceholder)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "${car.title} ${car.year}",
                                fontFamily = DmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = KarsyNavy
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${car.price} ${car.currency}",
                                fontFamily = Outfit,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = KarsyTeal
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                sub,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                color = KarsyMid
                            )
                        }
                        RowChevron()
                    }
                    if (idx < items.lastIndex) RowDivider()
                }
            }
        }
    }
}
