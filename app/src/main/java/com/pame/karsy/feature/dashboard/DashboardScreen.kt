package com.pame.karsy.feature.dashboard

import kotlin.coroutines.cancellation.CancellationException
import com.pame.karsy.core.locale.rememberTraduccion
import com.pame.karsy.core.locale.texto
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.outlined.FileDownload
import android.widget.Toast
import com.pame.karsy.core.components.KarsyBackButton
import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.pame.karsy.data.model.EstadoAuto
import com.pame.karsy.data.repository.SellerStats
import com.pame.karsy.feature.profile.ImagePlaceholder
import com.pame.karsy.feature.profile.ProfileAvatar
import com.pame.karsy.feature.profile.FOTO_EN_REVISION_ALPHA
import com.pame.karsy.feature.profile.FotoEnRevision
import com.pame.karsy.feature.profile.carStatusLabel
import com.pame.karsy.feature.profile.soldViaLabel

/** Panel del vendedor ("Mi Panel"): métricas, cómo van sus publicaciones y vehículos publicados. */
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportar by remember { mutableStateOf(false) }
    // Mientras se genera el archivo: el botón se deshabilita y se ve un indicador.
    var exportando by remember { mutableStateOf(false) }
    if (exportar) {
        val stats = vm.stats
        ExportarDialog(
            onDismiss = { exportar = false },
            onElegir = { pdf ->
                exportar = false
                if (stats != null) {
                    val destacadas = vm.myListings.count { it.featured && it.status == EstadoAuto.ACTIVO }
                    val nombre = vm.displayName
                    val listings = vm.myListings
                    // El archivo se arma fuera del hilo principal; el menú de compartir se abre de vuelta en él.
                    exportando = true
                    scope.launch {
                        runCatching {
                            withContext(Dispatchers.IO) {
                                if (pdf) ExportarPanel.generarPdf(context, nombre, stats, listings, destacadas)
                                else ExportarPanel.generarExcel(context, nombre, stats, listings, destacadas)
                            }
                        }.onSuccess { ExportarPanel.compartir(context, it) }
                            .onFailure {
                                if (it is CancellationException) throw it // se salió de Mi panel: sin aviso de error
                                Toast.makeText(context, texto(R.string.profile_dashboard_export_error), Toast.LENGTH_LONG).show()
                            }
                        exportando = false
                    }
                }
            }
        )
    }
    // Los avisos de "Destacar" cubren la pantalla: sin barra inferior mientras están abiertos.
    HideBottomBarWhile(vm.destacarCar != null || vm.solicitudCar != null)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            DashboardHeader(
                name = vm.displayName,
                avatarUrl = vm.avatarUrl,
                onBack = onBack,
                // Exportar solo cuando ya hay datos que exportar.
                onExport = if (vm.stats != null && !vm.loading && !exportando) ({ exportar = true }) else null,
                exportando = exportando
            )

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
                        // Esqueleto: tarjetas de estadísticas + rendimiento + una publicación.
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SkeletonBlock(110.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                            SkeletonBlock(110.dp, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                        }
                        SkeletonBlock(170.dp, shape = RoundedCornerShape(16.dp))
                        SkeletonCarCard(imageHeight = 110.dp)
                    }
                    if (stats != null) {
                        // Solo las que se ven en portada: un destacado vigente de un auto vendido o pausado no cuenta.
                        StatsRow(stats, vm.myListings.count { it.featured && it.status == EstadoAuto.ACTIVO })
                        PerformanceCard(stats, vm.myListings, vm.loading, onCarClick)
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
private fun DashboardHeader(name: String, avatarUrl: String?, onBack: () -> Unit, onExport: (() -> Unit)?, exportando: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 12.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KarsyBackButton(onClick = onBack, contentDescription = stringResource(R.string.profile_dashboard_back_cd))
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
        if (exportando) {
            CircularProgressIndicator(color = KarsyTeal, strokeWidth = 2.dp, modifier = Modifier.padding(12.dp).size(24.dp))
        } else if (onExport != null) {
            IconButton(onClick = onExport) {
                Icon(
                    Icons.Outlined.FileDownload,
                    contentDescription = stringResource(R.string.profile_dashboard_export),
                    tint = KarsyInk
                )
            }
        }
        ProfileAvatar(
            name = name,
            avatarUrl = avatarUrl,
            size = 40.dp,
            initialsSize = 14.sp,
            modifier = Modifier.border(2.dp, KarsyTeal, CircleShape)
        )
    }
}

/**
 * Estado de las publicaciones en 2×2: activas, vendidas, en revisión y destacadas.
 * Vistas, favoritos y contactos van en el embudo de "Cómo van tus publicaciones".
 */
@Composable
private fun StatsRow(stats: SellerStats, destacadas: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            SmallStatCard(
                stringResource(R.string.profile_dashboard_kpi_active),
                stats.activas.toString(),
                KarsyTeal,
                stringResource(R.string.profile_dashboard_kpi_active_sub),
                Modifier.weight(1f).fillMaxHeight()
            )
            SmallStatCard(
                stringResource(R.string.profile_dashboard_kpi_sold),
                stats.vendidas.toString(),
                KarsyInk,
                stringResource(R.string.profile_dashboard_kpi_sold_sub),
                Modifier.weight(1f).fillMaxHeight()
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(IntrinsicSize.Min)) {
            SmallStatCard(
                stringResource(R.string.profile_dashboard_kpi_in_review),
                stats.pendientes.toString(),
                KarsyInk,
                stringResource(R.string.profile_dashboard_kpi_in_review_sub),
                Modifier.weight(1f).fillMaxHeight()
            )
            SmallStatCard(
                stringResource(R.string.profile_dashboard_kpi_featured),
                destacadas.toString(),
                KarsyTeal,
                stringResource(R.string.profile_dashboard_kpi_featured_sub),
                Modifier.weight(1f).fillMaxHeight()
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

/** Elegir formato: Excel (.xlsx) para trabajar los números o PDF para presentarlos. */
@Composable
private fun ExportarDialog(onDismiss: () -> Unit, onElegir: (pdf: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsySurface,
        title = {
            Text(
                stringResource(R.string.profile_dashboard_export_title),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.profile_dashboard_export_sub), fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid)
                OutlinedButton(onClick = { onElegir(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.profile_dashboard_export_csv), fontFamily = DmSans, color = KarsyInk)
                }
                Button(
                    onClick = { onElegir(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy)
                ) {
                    Text(stringResource(R.string.profile_dashboard_export_pdf), fontFamily = DmSans, color = KarsyWhite)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_dashboard_export_cancel), fontFamily = DmSans, color = KarsyMid)
            }
        }
    )
}

/**
 * "Cómo van tus publicaciones" (totales): arriba el embudo vistas → favoritos →
 * contactos de todo el perfil; abajo una barra por publicación, la más vista primero.
 */
@Composable
private fun PerformanceCard(stats: SellerStats, listings: List<Car>, loading: Boolean, onCarClick: (Long) -> Unit) {
    var todas by remember { mutableStateOf(false) }
    // Nombre y año vienen de la lista cargada; los números, de la BD (0 si no hay).
    val porId = listings.associateBy { it.id }
    val filas = ExportarPanel.filas(stats, listings).map { it.second }
    val visibles = if (todas) filas else filas.take(6)
    val max = (filas.maxOfOrNull { it.vistas } ?: 0).coerceAtLeast(1)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(KarsySurface)
            .padding(16.dp)
    ) {
        Text(
            stringResource(R.string.profile_dashboard_performance_title),
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            stringResource(R.string.profile_dashboard_performance_sub),
            fontFamily = DmSans,
            fontSize = 11.sp,
            color = KarsyMid,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        // Embudo: cada paso un poco más angosto que el anterior.
        val pasos = listOf(
            Triple(stringResource(R.string.profile_dashboard_funnel_views), stats.embudoVistas, KarsyTeal) to null,
            Triple(stringResource(R.string.profile_dashboard_funnel_favorites), stats.embudoFavoritos, KarsyError) to
                stringResource(R.string.profile_dashboard_funnel_of_views, ExportarPanel.porcentaje(stats.embudoFavoritos, stats.embudoVistas)),
            Triple(stringResource(R.string.profile_dashboard_funnel_contacts), stats.embudoContactos, Color(0xFF22C55E)) to
                stringResource(R.string.profile_dashboard_funnel_of_views, ExportarPanel.porcentaje(stats.embudoContactos, stats.embudoVistas)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            pasos.forEachIndexed { i, (paso, sub) ->
                val (texto, n, color) = paso
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth(1f - i * 0.08f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KarsyBg)
                        .height(IntrinsicSize.Min)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(Modifier.width(4.dp).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(color))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(texto, fontFamily = DmSans, fontSize = 12.sp, color = KarsyInk)
                        if (sub != null) Text(sub, fontFamily = DmSans, fontSize = 10.sp, color = KarsyMid)
                    }
                    Text(n.toString(), fontFamily = Outfit, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        // Mientras carga la lista de publicaciones no se dice "sin publicaciones".
        if (filas.isEmpty() && !loading) {
            Text(
                stringResource(R.string.profile_dashboard_performance_empty),
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid
            )
        } else if (filas.isNotEmpty()) {
            visibles.forEach { r ->
                val car = porId.getValue(r.id)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCarClick(r.id) }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        "${car.brand} ${car.model} ${if (car.year > 0) car.year else ""}".trim(),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(KarsyBg)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth((r.vistas.toFloat() / max).coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(99.dp))
                                .background(KarsyTeal)
                        )
                    }
                    Text(
                        stringResource(R.string.profile_dashboard_performance_row, r.vistas, r.favoritos, r.contactos),
                        fontFamily = DmSans,
                        fontSize = 11.sp,
                        color = KarsyMid
                    )
                }
            }
            if (filas.size > 6) {
                Text(
                    if (todas) stringResource(R.string.profile_dashboard_performance_less)
                    else stringResource(R.string.profile_dashboard_performance_all, filas.size),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { todas = !todas }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                )
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
            // Nueva en revisión: foto atenuada (todavía nadie más la ve).
            val enRevision = car.status == EstadoAuto.PENDIENTE
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.title,
                contentScale = ContentScale.Crop,
                colorFilter = if (enRevision) FotoEnRevision else null,
                alpha = if (enRevision) FOTO_EN_REVISION_ALPHA else 1f,
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
                    .background((if (car.status == EstadoAuto.ACTIVO) KarsyTeal else KarsyNavy).copy(alpha = 0.85f))
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
            if (car.status == EstadoAuto.VENDIDO) soldViaLabel(car.soldVia)?.let {
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
                    stringResource(R.string.detail_disabled_reason, rememberTraduccion(reason)),
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
                val puedeDestacar = car.status == EstadoAuto.ACTIVO && !car.featuredPending && (!car.featured || renovar)
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
