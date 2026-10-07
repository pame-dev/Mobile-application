package com.pame.karsy.feature.favorites

import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.StarBadge
import com.pame.karsy.core.components.SkeletonCarCard
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyCardOutline
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car
import com.pame.karsy.feature.home.FilterSheet
import com.pame.karsy.feature.home.HomeFilters

private val CardBorder: Color get() = KarsyCardOutline

/** Lista de vehículos guardados por el usuario (WebFavoritesView del mockup). */
@Composable
fun FavoritesScreen(
    userMode: UserMode,
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    vm: FavoritesViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    HideBottomBarWhile(filtersOpen)
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val displayed = vm.visibleCars

    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            SubHeader(title = stringResource(R.string.home_favorites_title), onBack = onBack)

            KarsyPullToRefresh(onRefresh = { done -> vm.load(done) }, modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp + LocalBottomBarSpace.current),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            SearchRow(search = vm.query, onSearch = { vm.query = it }, onFilter = { filtersOpen = true })
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    pluralStringResource(R.plurals.home_vehicles_saved, displayed.size, displayed.size),
                                    fontFamily = Outfit,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KarsyInk
                                )
                                Text(
                                    vm.sortLabel,
                                    fontFamily = DmSans,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KarsyTeal,
                                    modifier = Modifier.clickable(onClick = vm::nextSort)
                                )
                            }
                            (errorMsg ?: vm.error)?.let {
                                Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyError, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }

                    if (vm.loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                repeat(2) { SkeletonCarCard() }
                            }
                        }
                    }

                    items(displayed, key = { it.id }) { car ->
                        FavoriteCard(
                            car = car,
                            onClick = { onCarClick(car.id) },
                            onRemove = { vm.remove(car.id) { errorMsg = it } },
                            // Al quitar un favorito la tarjeta se desvanece y las demás se reacomodan.
                            modifier = Modifier.animateItem(
                                fadeInSpec = tween(200),
                                placementSpec = tween(300),
                                fadeOutSpec = tween(250)
                            )
                        )
                    }

                    if (displayed.isEmpty() && !vm.loading) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            // Sin favoritos guardados vs. favoritos que no coinciden con búsqueda/filtros.
                            if (vm.cars.isEmpty()) EmptyState(
                                emoji = "🤍",
                                title = stringResource(R.string.favorites_empty_title),
                                subtitle = stringResource(R.string.favorites_empty_subtitle)
                            ) else EmptyState(
                                emoji = "🔍",
                                title = stringResource(R.string.home_no_results),
                                subtitle = stringResource(R.string.home_try_another_search)
                            )
                        }
                    }
                }
            }
        }

        FilterSheet(
            visible = filtersOpen,
            userMode = userMode,
            filters = vm.filters,
            options = vm.filterOptions,
            onApply = { vm.filters = it },
            onDismiss = { filtersOpen = false },
            onRegister = {}, // Favoritos solo existe con sesión iniciada
            onClear = { vm.filters = HomeFilters() }
        )
    }
}

@Composable
private fun SearchRow(search: String, onSearch: (String) -> Unit, onFilter: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(48.dp)
                .clip(shape)
                .background(KarsySurface)
                .border(1.5.dp, KarsyBorder, shape)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = search,
                onValueChange = onSearch,
                singleLine = true,
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 15.sp, color = KarsyCharcoal),
                cursorBrush = SolidColor(KarsyTeal),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (search.isEmpty()) {
                            Text(stringResource(R.string.home_favorites_search_placeholder), fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid, maxLines = 1)
                        }
                        inner()
                    }
                }
            )
        }
        Row(
            Modifier
                .height(48.dp)
                .clip(shape)
                .background(KarsySurface)
                .border(1.5.dp, KarsyBorder, shape)
                .clickable(onClick = onFilter)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.FilterList, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(18.dp))
            Text(stringResource(R.string.home_filter), fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KarsyTeal)
        }
    }
}

private val SoldBadge = Color(0xFF027A48)

@Composable
private fun FavoriteCard(
    car: Car,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val sold = car.status == "Vendido"
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.08f), spotColor = KarsyNavy.copy(alpha = 0.14f))
            .clip(shape)
            .background(KarsySurface)
            .border(1.dp, CardBorder, shape)
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(160.dp)
                .background(KarsyBg)
        ) {
            AsyncImage(
                model = car.imageUrl,
                contentDescription = car.model,
                contentScale = ContentScale.Crop,
                // Vendido: la foto se atenúa para que se note que ya no está disponible.
                alpha = if (sold) 0.55f else 1f,
                modifier = Modifier.fillMaxSize()
            )
            StarBadge(
                text = if (sold) stringResource(R.string.profile_status_sold) else car.condition,
                background = if (sold) SoldBadge else KarsyNavy.copy(alpha = 0.75f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(34.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.92f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = stringResource(R.string.home_remove_favorite),
                    tint = KarsyInk,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp)) {
            Text(
                car.title,
                fontFamily = Outfit,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "${car.year} · ${car.kilometraje}",
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyMid,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(car.price, fontFamily = Outfit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(stringResource(R.string.home_see_more), fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(emoji: String, title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 40.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text(
            title,
            textAlign = TextAlign.Center,
            fontFamily = Outfit,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            subtitle,
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyMid,
            textAlign = TextAlign.Center
        )
    }
}
