package com.pame.karsy.feature.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Panel del vendedor ("Mi Panel"): métricas, interés semanal y vehículos publicados. */
@Composable
fun DashboardScreen(onBack: () -> Unit, onNewPublication: () -> Unit) {
    val vm: DashboardViewModel = viewModel()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(avatarUrl = vm.avatarUrl, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatsRow(vm)
                WeeklyInterestCard(vm)
                MyListingsSection(
                    listings = vm.myListings,
                    onDestacar = vm::askDestacar,
                    onNewPublication = onNewPublication
                )
            }
        }

        AnimatedVisibility(visible = vm.destacarCar != null, enter = fadeIn(), exit = fadeOut()) {
            // Se conserva el último nombre mientras se anima la salida.
            val name = vm.destacarCar
            if (name != null) {
                DestacarDialog(
                    carName = name,
                    onConfirm = vm::confirmDestacar,
                    onCancel = vm::cancelDestacar
                )
            }
        }
        AnimatedVisibility(visible = vm.solicitudCar != null, enter = fadeIn(), exit = fadeOut()) {
            val name = vm.solicitudCar
            if (name != null) {
                SolicitudEnviadaDialog(carName = name, onClose = vm::closeSolicitud)
            }
        }
    }
}

@Composable
private fun DashboardHeader(avatarUrl: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = "Volver al perfil",
                tint = KarsyNavy,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            "Mi Panel",
            fontFamily = Outfit,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyNavy,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f)
        )
        AsyncImage(
            model = avatarUrl,
            contentDescription = "Avatar",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(2.dp, KarsyTeal, CircleShape)
        )
    }
}

@Composable
private fun StatsRow(vm: DashboardViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(KarsyWhite)
                .padding(16.dp)
        ) {
            CardLabel("VENTAS TOTALES")
            Text(
                vm.totalSales.toString(),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                vm.salesTrend,
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = KarsyTeal,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
            )
            Spacer(Modifier.weight(1f))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                drawSparkline(vm.salesMonths, size.width, size.height, strokeWidth = 2.dp, fillAlpha = 0.2f)
            }
        }

        Column(
            modifier = Modifier.width(110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard("PUBLICADOS", vm.publishedCount.toString(), KarsyNavy, "Vehículos", Modifier.weight(1f))
            SmallStatCard("CONSULTAS", vm.inquiries.toString(), KarsyTeal, "Contactos", Modifier.weight(1f))
        }
    }
}

@Composable
private fun SmallStatCard(label: String, value: String, valueColor: Color, caption: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardLabel(label)
        Text(
            value,
            fontFamily = Outfit,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            caption,
            fontFamily = DmSans,
            fontSize = 11.sp,
            color = KarsyMid,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun CardLabel(text: String) {
    Text(text, fontFamily = DmSans, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = KarsyMid)
}

@Composable
private fun WeeklyInterestCard(vm: DashboardViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
            .padding(16.dp)
    ) {
        Text(
            "Interés en la semana",
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Favoritos agregados por día",
            fontFamily = DmSans,
            fontSize = 11.sp,
            color = KarsyMid,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        ) {
            val chartH = size.height - 16.dp.toPx()
            drawSparkline(vm.favWeekly, size.width, chartH, strokeWidth = 2.5.dp, fillAlpha = 0.28f)
            val points = sparklinePoints(vm.favWeekly, size.width, chartH, 6.dp.toPx())
            val maxV = vm.favWeekly.max()
            points.forEachIndexed { i, p ->
                val isMax = vm.favWeekly[i] == maxV
                if (isMax) drawCircle(KarsyTeal.copy(alpha = 0.2f), radius = 8.dp.toPx(), center = p)
                drawCircle(KarsyTeal, radius = if (isMax) 5.dp.toPx() else 3.5.dp.toPx(), center = p)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            vm.weekDays.forEach { d ->
                Text(d, fontFamily = DmSans, fontSize = 11.sp, color = KarsyMid)
            }
        }
    }
}

@Composable
private fun MyListingsSection(
    listings: List<MyListing>,
    onDestacar: (String) -> Unit,
    onNewPublication: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Mis Vehículos Publicados",
                fontFamily = Outfit,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                modifier = Modifier.weight(1f)
            )
            // Acceso a "Publicar vehículo" (no aparece en el mockup, pero la ruta lo requiere).
            Text(
                "+ Publicar",
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNewPublication)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listings.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { car ->
                        ListingCard(car, onDestacar, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ListingCard(car: MyListing, onDestacar: (String) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
    ) {
        AsyncImage(
            model = car.imageUrl,
            contentDescription = car.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Text(
                car.name,
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyCharcoal
            )
            Text(
                car.price,
                fontFamily = DmSans,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyTeal,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { onDestacar(car.name) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text("Destacar", fontFamily = Outfit, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Puntos de la gráfica (misma fórmula que sparklinePath del mockup). */
private fun sparklinePoints(data: List<Float>, w: Float, h: Float, pad: Float): List<Offset> {
    val minV = data.min()
    val maxV = data.max()
    val range = (maxV - minV).takeIf { it != 0f } ?: 1f
    return data.mapIndexed { i, v ->
        Offset(
            x = i.toFloat() / (data.size - 1) * w,
            y = h - pad - (v - minV) / range * (h - pad * 2)
        )
    }
}

private fun DrawScope.drawSparkline(data: List<Float>, w: Float, h: Float, strokeWidth: Dp, fillAlpha: Float) {
    val points = sparklinePoints(data, w, h, 6.dp.toPx())
    val line = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }
    val fill = Path().apply {
        addPath(line)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        fill,
        Brush.verticalGradient(
            listOf(KarsyTeal.copy(alpha = fillAlpha), KarsyTeal.copy(alpha = 0f)),
            startY = 0f,
            endY = h
        )
    )
    drawPath(
        line,
        KarsyTeal,
        style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}
