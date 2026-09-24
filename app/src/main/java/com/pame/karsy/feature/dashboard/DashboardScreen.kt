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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.repository.SellerStats
import com.pame.karsy.feature.profile.ImagePlaceholder
import com.pame.karsy.feature.profile.ProfileAvatar

/** Panel del vendedor ("Mi Panel"): métricas, interés semanal y vehículos publicados. */
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onNewPublication: () -> Unit,
    onCarClick: (Long) -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    // Se recarga al volver (p. ej. después de publicar).
    LaunchedEffect(Unit) { vm.load() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(name = vm.displayName, avatarUrl = vm.avatarUrl, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                vm.error?.let {
                    Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyError)
                }
                val stats = vm.stats
                if (stats == null && vm.loading) {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarsyTeal)
                    }
                }
                if (stats != null) {
                    StatsRow(stats, vm.viewsTrend)
                    WeeklyInterestCard(stats.favoritosSemana, stats.diasSemana)
                }
                MyListingsSection(
                    listings = vm.myListings,
                    onDestacar = vm::askDestacar,
                    onNewPublication = onNewPublication,
                    onCarClick = onCarClick
                )
            }
        }

        AnimatedVisibility(visible = vm.destacarCar != null, enter = fadeIn(), exit = fadeOut()) {
            // Se conserva el último auto mientras se anima la salida.
            val car = vm.destacarCar
            if (car != null) {
                DestacarDialog(
                    carName = "${car.title} ${car.year}",
                    onConfirm = vm::confirmDestacar,
                    onCancel = vm::cancelDestacar
                )
            }
        }
        AnimatedVisibility(visible = vm.solicitudCar != null, enter = fadeIn(), exit = fadeOut()) {
            val car = vm.solicitudCar
            if (car != null) {
                SolicitudEnviadaDialog(carName = "${car.title} ${car.year}", onClose = vm::closeSolicitud)
            }
        }
    }
}

@Composable
private fun DashboardHeader(name: String, avatarUrl: String?, onBack: () -> Unit) {
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
        ProfileAvatar(
            name = name,
            avatarUrl = avatarUrl,
            size = 40.dp,
            initialsSize = 14.sp,
            modifier = Modifier.border(2.dp, KarsyTeal, CircleShape)
        )
    }
}

@Composable
private fun StatsRow(stats: SellerStats, trend: String) {
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
            // No hay tabla de ventas: la métrica principal son las vistas de detalle.
            CardLabel("VISTAS TOTALES")
            Text(
                stats.vistas.toString(),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                trend,
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
                drawSparkline(stats.vistasPorMes, size.width, size.height, strokeWidth = 2.dp, fillAlpha = 0.2f)
            }
        }

        Column(
            modifier = Modifier.width(110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatCard("PUBLICADOS", stats.publicadas.toString(), KarsyNavy, "Vehículos", Modifier.weight(1f))
            SmallStatCard("CONSULTAS", stats.contactos.toString(), KarsyTeal, "Contactos", Modifier.weight(1f))
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
private fun WeeklyInterestCard(favWeekly: List<Float>, weekDays: List<String>) {
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
            drawSparkline(favWeekly, size.width, chartH, strokeWidth = 2.5.dp, fillAlpha = 0.28f)
            val points = sparklinePoints(favWeekly, size.width, chartH, 6.dp.toPx())
            val maxV = favWeekly.maxOrNull() ?: 0f
            points.forEachIndexed { i, p ->
                val isMax = maxV > 0f && favWeekly[i] == maxV
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
            weekDays.forEach { d ->
                Text(d, fontFamily = DmSans, fontSize = 11.sp, color = KarsyMid)
            }
        }
    }
}

@Composable
private fun MyListingsSection(
    listings: List<Car>,
    onDestacar: (Car) -> Unit,
    onNewPublication: () -> Unit,
    onCarClick: (Long) -> Unit,
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
            if (listings.isEmpty()) {
                Text(
                    "Todavía no publicas ningún vehículo. Toca \"+ Publicar\" para crear el primero.",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    color = KarsyMid
                )
            }
            listings.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { car ->
                        ListingCard(car, onDestacar, onCarClick, Modifier.weight(1f).fillMaxHeight())
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ListingCard(car: Car, onDestacar: (Car) -> Unit, onCarClick: (Long) -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KarsyWhite)
            .clickable { onCarClick(car.id) }
    ) {
        Box {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(ImagePlaceholder)
            )
            Text(
                car.status,
                fontFamily = DmSans,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyWhite,
                modifier = Modifier
                    .padding(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background((if (car.status == "Activo") KarsyTeal else KarsyNavy).copy(alpha = 0.85f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Text(
                "${car.title} ${car.year}",
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
            // Solo un anuncio activo puede destacarse, y una solicitud a la vez.
            val puedeDestacar = car.status == "Activo" && !car.featured && !car.featuredPending
            Button(
                onClick = { onDestacar(car) },
                enabled = puedeDestacar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Text(
                    when {
                        car.featured -> "Destacado"
                        car.featuredPending -> "Solicitud enviada"
                        else -> "Destacar"
                    },
                    fontFamily = Outfit,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/** Puntos de la gráfica (misma fórmula que sparklinePath del mockup). */
private fun sparklinePoints(data: List<Float>, w: Float, h: Float, pad: Float): List<Offset> {
    if (data.isEmpty()) return emptyList()
    val minV = data.min()
    val maxV = data.max()
    val range = (maxV - minV).takeIf { it != 0f } ?: 1f
    val pasos = (data.size - 1).coerceAtLeast(1)
    return data.mapIndexed { i, v ->
        Offset(
            x = i.toFloat() / pasos * w,
            y = h - pad - (v - minV) / range * (h - pad * 2)
        )
    }
}

private fun DrawScope.drawSparkline(data: List<Float>, w: Float, h: Float, strokeWidth: Dp, fillAlpha: Float) {
    val points = sparklinePoints(data, w, h, 6.dp.toPx())
    if (points.isEmpty()) return
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
