package com.pame.karsy.feature.dashboard

import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.SkeletonBlock
import com.pame.karsy.core.session.PublicacionesOcultas
import com.pame.karsy.core.components.SkeletonCarCard
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
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
import com.pame.karsy.feature.profile.carStatusLabel
import com.pame.karsy.feature.profile.soldViaLabel

/** Panel del vendedor ("Mi Panel"): métricas, interés semanal y vehículos publicados. */
@Composable
fun DashboardScreen(
    onBack: () -> Unit,
    onNewPublication: () -> Unit,
    onEditPublication: (Long) -> Unit,
    onCarClick: (Long) -> Unit,
    vm: DashboardViewModel = viewModel(),
) {
    // Se recarga al volver (p. ej. después de publicar).
    LaunchedEffect(Unit) { vm.load() }
    // Los avisos de "Destacar" cubren la pantalla: sin barra inferior mientras están abiertos.
    HideBottomBarWhile(vm.destacarCar != null || vm.solicitudCar != null)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(name = vm.displayName, avatarUrl = vm.avatarUrl, onBack = onBack)

            KarsyPullToRefresh(onRefresh = { done -> vm.load(done) }, modifier = Modifier.weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp + LocalBottomBarSpace.current),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    vm.error?.let {
                        Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyError)
                    }
                    val stats = vm.stats
                    if (stats == null && vm.loading) {
                        // Esqueleto: tarjetas de estadísticas + gráfica semanal + una publicación.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SkeletonBlock(110.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            SkeletonBlock(110.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                        SkeletonBlock(170.dp, shape = RoundedCornerShape(16.dp))
                        SkeletonCarCard(imageHeight = 110.dp)
                    }
                    if (stats != null) {
                        StatsRow(stats, vm.viewsTrend)
                        WeeklyInterestCard(stats.favoritosSemana, stats.diasSemana)
                    }
                    // Las que el dueño ocultó de su perfil van en su propia sección, al final.
                    val (hidden, visible) = vm.myListings.partition(PublicacionesOcultas::estaOculta)
                    MyListingsSection(
                        listings = visible,
                        favoriteCounts = vm.favoriteCounts,
                        onDestacar = vm::askDestacar,
                        onEdit = onEditPublication,
                        onNewPublication = onNewPublication,
                        onCarClick = onCarClick
                    )
                    if (hidden.isNotEmpty()) HiddenListingsSection(
                        listings = hidden,
                        favoriteCounts = vm.favoriteCounts,
                        onCarClick = onCarClick
                    )
                }
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
                contentDescription = stringResource(R.string.profile_dashboard_back_cd),
                tint = KarsyInk,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            stringResource(R.string.profile_dashboard_title),
            fontFamily = Outfit,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyInk,
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
                .background(KarsySurface)
                .padding(16.dp)
        ) {
            // No hay tabla de ventas: la métrica principal son las vistas de detalle.
            CardLabel(stringResource(R.string.profile_dashboard_total_views))
            Text(
                stats.vistas.toString(),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
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
            SmallStatCard(
                stringResource(R.string.profile_dashboard_published),
                stats.publicadas.toString(),
                KarsyInk,
                stringResource(R.string.profile_dashboard_vehicles),
                Modifier.weight(1f)
            )
            SmallStatCard(
                stringResource(R.string.profile_dashboard_inquiries),
                stats.contactos.toString(),
                KarsyTeal,
                stringResource(R.string.profile_dashboard_contacts),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SmallStatCard(label: String, value: String, valueColor: Color, caption: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KarsySurface)
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
            .background(KarsySurface)
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.profile_dashboard_weekly_interest),
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            stringResource(R.string.profile_dashboard_favorites_per_day),
            fontFamily = DmSans,
            fontSize = 11.sp,
            color = KarsyMid,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            pluralStringResource(
                R.plurals.profile_dashboard_favorites_total,
                favWeekly.sum().toInt(),
                favWeekly.sum().toInt()
            ),
            fontFamily = DmSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyTeal,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        val textMeasurer = rememberTextMeasurer()
        val valueStyle = TextStyle(fontFamily = DmSans, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
        val dayStyle = TextStyle(fontFamily = DmSans, fontSize = 11.sp, color = KarsyMid)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            // Barras desde 0 para que la altura sea proporcional a la cantidad real de favoritos
            val count = maxOf(favWeekly.size, weekDays.size)
            if (count == 0) return@Canvas
            val valueSpace = 16.dp.toPx()
            val bottom = size.height - 20.dp.toPx()
            val plotH = bottom - valueSpace
            val maxV = (favWeekly.maxOrNull() ?: 0f).coerceAtLeast(1f)
            val slot = size.width / count
            val barW = minOf(28.dp.toPx(), slot * 0.55f)
            drawLine(KarsyBg, Offset(0f, bottom), Offset(size.width, bottom), strokeWidth = 1.dp.toPx())
            for (i in 0 until count) {
                val v = favWeekly.getOrElse(i) { 0f }
                val center = slot * i + slot / 2f
                val barH = v / maxV * plotH
                val isMax = v > 0f && v == favWeekly.maxOrNull()
                if (barH > 0f) {
                    drawRoundRect(
                        color = if (isMax) KarsyTeal else KarsyTeal.copy(alpha = 0.45f),
                        topLeft = Offset(center - barW / 2f, bottom - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                }
                val valueLayout = textMeasurer.measure(v.toInt().toString(), valueStyle)
                drawText(
                    valueLayout,
                    topLeft = Offset(
                        center - valueLayout.size.width / 2f,
                        bottom - barH - 3.dp.toPx() - valueLayout.size.height
                    )
                )
                weekDays.getOrNull(i)?.let { d ->
                    val dayLayout = textMeasurer.measure(d, dayStyle)
                    drawText(
                        dayLayout,
                        topLeft = Offset(center - dayLayout.size.width / 2f, bottom + 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListingsSection(
    listings: List<Car>,
    favoriteCounts: Map<Long, Int>,
    onDestacar: (Car) -> Unit,
    onEdit: (Long) -> Unit,
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
                stringResource(R.string.profile_dashboard_my_listings),
                fontFamily = Outfit,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
                modifier = Modifier.weight(1f)
            )
            // Acceso a "Publicar vehículo" (no aparece en el mockup, pero la ruta lo requiere).
            Text(
                stringResource(R.string.profile_dashboard_publish),
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
                    stringResource(R.string.profile_dashboard_no_listings),
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    color = KarsyMid
                )
            }
            ListingsGrid(listings) { car, modifier ->
                ListingCard(car, favoriteCounts[car.id] ?: 0, onDestacar, onEdit, onCarClick, modifier)
            }
        }
    }
}

/** Rechazadas / deshabilitadas que el dueño ocultó de su perfil; desde aquí se vuelven a mostrar. */
@Composable
private fun HiddenListingsSection(
    listings: List<Car>,
    favoriteCounts: Map<Long, Int>,
    onCarClick: (Long) -> Unit,
) {
    Column {
        Text(
            stringResource(R.string.profile_dashboard_hidden_listings),
            fontFamily = Outfit,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyInk
        )
        Text(
            stringResource(R.string.profile_dashboard_hidden_desc),
            fontFamily = DmSans,
            fontSize = 12.sp,
            color = KarsyMid,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )
        ListingsGrid(listings) { car, modifier ->
            ListingCard(
                car, favoriteCounts[car.id] ?: 0, onDestacar = {}, onEdit = {}, onCarClick = onCarClick, modifier = modifier,
                onUnhide = { PublicacionesOcultas.mostrar(car.id) }
            )
        }
    }
}

/** Tarjetas en dos columnas con la misma altura por fila. */
@Composable
private fun ListingsGrid(listings: List<Car>, card: @Composable (Car, Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listings.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { car -> card(car, Modifier.weight(1f).fillMaxHeight()) }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ListingCard(
    car: Car,
    favorites: Int,
    onDestacar: (Car) -> Unit,
    onEdit: (Long) -> Unit,
    onCarClick: (Long) -> Unit,
    modifier: Modifier,
    /** Si no es null, la tarjeta está en "Publicaciones ocultas" y su botón la vuelve a mostrar. */
    onUnhide: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(KarsySurface)
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
                carStatusLabel(car.status),
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
            // Usuarios que la guardaron en favoritos.
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = KarsyError, modifier = Modifier.size(13.dp))
                Text(
                    pluralStringResource(R.plurals.profile_dashboard_favorites, favorites, favorites),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            // Vendida: dónde se vendió (dentro o fuera de la app).
            if (car.status == "Vendido") soldViaLabel(car.soldVia)?.let {
                Text(
                    it,
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            // Motivo del admin; el texto completo se ve al abrir la publicación (o al corregirla).
            (car.disabledReason ?: car.rejectedReason)?.let { reason ->
                Text(
                    stringResource(R.string.detail_disabled_reason, reason),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = KarsyError,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            if (onUnhide != null) {
                OutlinedButton(
                    onClick = onUnhide,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, KarsyNavy),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KarsyNavy),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text(
                        stringResource(R.string.profile_dashboard_unhide),
                        fontFamily = Outfit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            // Rechazada: en lugar de destacar, se corrige y se reenvía a revisión.
            } else if (car.rejectedReason != null) {
                Button(
                    onClick = { onEdit(car.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyTeal, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text(
                        stringResource(R.string.profile_dashboard_fix_resubmit),
                        fontFamily = Outfit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Solo un anuncio activo puede destacarse, y una solicitud a la vez.
                // Si el destacado termina en 3 días o menos, se puede renovar.
                val renovar = car.featured && car.featuredEndsSoon && !car.featuredPending
                val puedeDestacar = car.status == "Activo" && !car.featuredPending && (!car.featured || renovar)
                Button(
                    onClick = { onDestacar(car) },
                    enabled = puedeDestacar,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (renovar) KarsyTeal else KarsyNavy, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(34.dp)
                ) {
                    Text(
                        when {
                            renovar -> stringResource(R.string.profile_dashboard_renew, car.featuredUntil.orEmpty())
                            car.featured && car.featuredUntil != null -> stringResource(R.string.profile_dashboard_featured_until, car.featuredUntil)
                            car.featured -> stringResource(R.string.profile_dashboard_featured)
                            car.featuredPending -> stringResource(R.string.profile_dashboard_request_sent)
                            else -> stringResource(R.string.profile_dashboard_feature)
                        },
                        fontFamily = Outfit,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
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
