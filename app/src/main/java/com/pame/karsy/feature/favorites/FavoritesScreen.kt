package com.pame.karsy.feature.favorites

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
import androidx.compose.material.icons.rounded.FavoriteBorder
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.StarBadge
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.theme.DmSans
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

private val CardBorder = Color(0xFFEEF1F4)

/** Lista de vehículos guardados por el usuario (WebFavoritesView del mockup). */
@Composable
fun FavoritesScreen(
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    vm: FavoritesViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    var search by rememberSaveable { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val favoriteIds = vm.favoriteIds

    val displayed = vm.sorted(vm.cars.filter {
        it.brand.contains(search, ignoreCase = true) || it.model.contains(search, ignoreCase = true)
    })

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(title = "❤️ Favoritos", onBack = onBack)

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 300.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    SearchRow(search = search, onSearch = { search = it })
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "${displayed.size} vehículos guardados",
                            fontFamily = Outfit,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KarsyNavy
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
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = KarsyTeal)
                    }
                }
            }

            items(displayed, key = { it.id }) { car ->
                val isFav = car.id in favoriteIds
                FavoriteCard(
                    car = car,
                    isFavorite = isFav,
                    onClick = { onCarClick(car.id) },
                    onToggleFavorite = { vm.toggle(car.id) { errorMsg = it } }
                )
            }

            if (displayed.isEmpty() && !vm.loading) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyState() }
            }
        }
    }
}

@Composable
private fun SearchRow(search: String, onSearch: (String) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(48.dp)
                .clip(shape)
                .background(KarsyWhite)
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
                            Text("Buscar en favoritos…", fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid, maxLines = 1)
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
                .background(KarsyWhite)
                .border(1.5.dp, KarsyBorder, shape)
                .clickable { /* TODO: abrir filtros de favoritos */ }
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.FilterList, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(18.dp))
            Text("Filtrar", fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KarsyTeal)
        }
    }
}

@Composable
private fun FavoriteCard(
    car: Car,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.08f), spotColor = KarsyNavy.copy(alpha = 0.14f))
            .clip(shape)
            .background(KarsyWhite)
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
                modifier = Modifier.fillMaxSize()
            )
            StarBadge(
                text = car.condition,
                background = KarsyNavy.copy(alpha = 0.75f),
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
                    .clickable(onClick = onToggleFavorite),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = if (isFavorite) "Quitar de favoritos" else "Agregar a favoritos",
                    tint = KarsyNavy,
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
                Text(car.price, fontFamily = Outfit, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = KarsyNavy)
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 7.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Ver más", fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🔍", fontSize = 40.sp, modifier = Modifier.padding(bottom = 12.dp))
        Text(
            "Sin resultados",
            fontFamily = Outfit,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Text(
            "Prueba con otro término de búsqueda.",
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyMid,
            textAlign = TextAlign.Center
        )
    }
}
