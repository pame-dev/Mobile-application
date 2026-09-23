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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.pame.karsy.data.repository.CarRepository

private data class HistoryItem(
    val carId: Int,
    val title: String,
    val price: String,
    val sub: String,
    val imageIndex: Int,
)

// TODO: leer el historial de vistas del usuario desde Supabase (publicaciones vistas recientemente).
private val historyItems = listOf(
    HistoryItem(6, "Honda Civic Sport 2021", "$298,000 MXN", "Visto hace 2 horas", 1),
    HistoryItem(5, "Toyota Corolla LE 2022", "$325,000 MXN", "Visto ayer", 0),
    HistoryItem(7, "Mazda 3 Sedán 2023", "$365,000 MXN", "Visto hace 3 días", 7),
    HistoryItem(1, "BMW Serie 3 320i 2023", "$685,000 MXN", "Visto hace 5 días", 2),
    HistoryItem(2, "Porsche Cayenne S 2022", "$1,450,000 MXN", "Visto hace 1 semana", 4),
    HistoryItem(8, "Nissan Versa Advance 2022", "$245,000 MXN", "Visto hace 1 semana", 3),
)

/** Historial de autos vistos (WebHistorialView del mockup). */
@Composable
fun HistoryScreen(onBack: () -> Unit, onCarClick: (Int) -> Unit) {
    val images = CarRepository.profilePostImages
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
            ProfileCard {
                historyItems.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCarClick(item.carId) }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AsyncImage(
                            model = images.getOrNull(item.imageIndex),
                            contentDescription = item.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(ImagePlaceholder)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.title,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = KarsyNavy
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.price,
                                fontFamily = Outfit,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = KarsyTeal
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.sub,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                color = KarsyMid
                            )
                        }
                        RowChevron()
                    }
                    if (idx < historyItems.lastIndex) RowDivider()
                }
            }
        }
    }
}
