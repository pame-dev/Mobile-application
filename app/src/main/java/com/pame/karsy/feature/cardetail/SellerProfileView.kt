package com.pame.karsy.feature.cardetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.data.repository.CarRepository

/** Perfil público del vendedor, mostrado a pantalla completa sobre el detalle. */
@Composable
internal fun SellerProfileView(detail: CarDetail, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    // TODO: cargar perfil real desde public.cuentas (+ perfiles_lote) y sus publicaciones activas.
    val posts = listOf(detail.car.imageUrl) + CarRepository.profilePostImages.take(5)

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            // Evita que los toques pasen al detalle que queda debajo.
            .clickable(enabled = false, onClick = {})
    ) {
        Column(Modifier.background(KarsyWhite)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onBack)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                ) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Regresar", tint = KarsyNavy, modifier = Modifier.size(24.dp))
                    Text("Inicio", fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = KarsyNavy)
                }
                Text(
                    "Perfil",
                    fontFamily = Outfit,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            HorizontalDivider(color = KarsyBorder, thickness = 1.dp)
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 40.dp)
        ) {
            val cardShape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(3.dp, cardShape, ambientColor = KarsyNavy, spotColor = KarsyNavy.copy(alpha = 0.25f))
                    .clip(cardShape)
                    .background(KarsyWhite)
                    .border(1.dp, KarsyBorder, cardShape)
            ) {
                Box {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(82.dp)
                        .background(Brush.linearGradient(listOf(KarsyNavy, KarsyTeal)))
                )
                // El avatar se monta 38 dp sobre el degradado (marginTop: -38 del mockup).
                Column(
                    Modifier.padding(start = 22.dp, end = 22.dp, top = 44.dp, bottom = 22.dp)
                ) {
                    Box(Modifier.padding(bottom = 12.dp).size(76.dp)) {
                        AsyncImage(
                            model = sellerAvatar(176),
                            contentDescription = detail.contactoNombre,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .shadow(4.dp, CircleShape)
                                .clip(CircleShape)
                                .background(KarsyWhite)
                                .border(4.dp, KarsyWhite, CircleShape)
                        )
                        Box(
                            Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(KarsyWhite)
                                .padding(3.dp)
                                .clip(CircleShape)
                                .background(KarsyTeal)
                        )
                    }
                    Text(
                        detail.contactoNombre,
                        fontFamily = Outfit,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyNavy,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        "@pamela",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyTeal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "Vendedora de autos confiable 🚗 · Ciudad de México",
                        fontFamily = DmSans,
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp,
                        color = KarsyTextSecondary,
                    )
                }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, cardShape, ambientColor = KarsyNavy, spotColor = KarsyNavy.copy(alpha = 0.25f))
                    .clip(cardShape)
                    .background(KarsyWhite)
                    .border(1.dp, KarsyBorder, cardShape)
            ) {
                Text(
                    "Publicaciones",
                    fontFamily = Outfit,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp)
                )
                HorizontalDivider(color = KarsyBg, thickness = 1.dp)
                Column(
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(3.dp)
                ) {
                    posts.chunked(3).forEachIndexed { rowIdx, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            row.forEachIndexed { i, src ->
                                AsyncImage(
                                    model = src,
                                    contentDescription = "Publicación ${rowIdx * 3 + i + 1} de ${detail.contactoNombre}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
