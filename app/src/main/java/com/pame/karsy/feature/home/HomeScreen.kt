package com.pame.karsy.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.HeartIcon
import com.pame.karsy.core.components.KarsyBrand
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyDisabled
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyNavyLight
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.repository.CarRepository

/** Pantalla principal del marketplace (WebHomeScreen del mockup). */
@Composable
fun HomeScreen(
    userMode: UserMode,
    onCarClick: (Int) -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    onAdminPanel: () -> Unit,
    onPublish: () -> Unit,
    onRegister: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }
    // TODO: cargar los favoritos del usuario desde public.favoritos
    val favoriteIds = remember { mutableStateListOf<Int>() }
    var sortBy by rememberSaveable { mutableStateOf(ORDEN_OPCIONES.first()) }

    // TODO: obtener destacados y publicaciones activas desde Supabase
    val featured = CarRepository.featuredCars
    val allCars = CarRepository.allCars

    val isVisitor = !userMode.isLoggedIn
    val showHeart = !userMode.isAdmin

    val toggleFavorite: (Int) -> Unit = { id ->
        if (isVisitor) {
            toastMsg = "Regístrate para guardar tus favoritos"
        } else {
            // TODO: insertar/borrar en public.favoritos
            if (id in favoriteIds) favoriteIds.remove(id) else favoriteIds.add(id)
        }
    }
    val goRegister = {
        toastMsg = null
        menuOpen = false
        onRegister()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                userMode = userMode,
                menuOpen = menuOpen,
                onToggleMenu = { menuOpen = !menuOpen },
                onRegister = goRegister,
                onFavorites = onFavorites,
                onProfile = onProfile,
                onAdminPanel = onAdminPanel
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) { HeroSection() }

                if (isVisitor) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        VisitorBanner(onRegister = goRegister, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                    }
                }

                // Vehículos destacados
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 8.dp)) {
                        SectionHeader(
                            title = "Vehículos destacados",
                            subtitle = "Selección especial de nuestra plataforma",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* TODO: abrir listado completo de destacados */ },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, KarsyBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = KarsyNavy),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Ver todos →", fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        LazyRow(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(featured, key = { it.id }) { car ->
                                FeaturedCarCard(
                                    car = car,
                                    isFavorite = car.id in favoriteIds,
                                    showHeart = showHeart,
                                    onClick = { onCarClick(car.id) },
                                    onToggleFavorite = { toggleFavorite(car.id) }
                                )
                            }
                        }
                    }
                }

                // Todos los vehículos
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(
                        title = "Todos los vehículos",
                        subtitle = "${allCars.size} vehículos disponibles",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        // TODO: ordenar la consulta de publicaciones según la opción elegida
                        KarsySelect(
                            selected = sortBy,
                            options = ORDEN_OPCIONES,
                            onSelect = { sortBy = it },
                            fillWidth = false,
                            container = KarsyWhite,
                            textColor = KarsyTextSecondary
                        )
                    }
                }
                items(allCars, key = { it.id }) { car ->
                    VehicleCard(
                        car = car,
                        isFavorite = car.id in favoriteIds,
                        showHeart = showHeart,
                        onClick = { onCarClick(car.id) },
                        onToggleFavorite = { toggleFavorite(car.id) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // Botón flotante "+"
        when {
            userMode.isAdmin -> Unit
            isVisitor -> AddFab(
                container = KarsyDisabled,
                content = KarsyMid,
                description = "Inicia sesión para agregar un vehículo",
                onClick = { toastMsg = "Regístrate para publicar tu vehículo" },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            else -> AddFab(
                container = KarsyTeal,
                content = KarsyWhite,
                description = "Agregar vehículo",
                onClick = onPublish,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        toastMsg?.let { msg ->
            RegisterToast(
                message = msg,
                onClose = { toastMsg = null },
                onRegister = goRegister,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        FilterSheet(
            visible = menuOpen,
            userMode = userMode,
            onDismiss = { menuOpen = false },
            onRegister = goRegister
        )
    }
}

@Composable
private fun HomeHeader(
    userMode: UserMode,
    menuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onRegister: () -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    onAdminPanel: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, spotColor = KarsyNavy.copy(alpha = 0.10f), ambientColor = KarsyNavy.copy(alpha = 0.07f))
            .background(KarsyWhite)
            .statusBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KarsyBrand()
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    !userMode.isLoggedIn -> Button(
                        onClick = onRegister,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = KarsyNavy.copy(alpha = 0.3f))
                    ) {
                        Text("Regístrate", fontFamily = Outfit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    userMode.isAdmin -> {
                        HeaderIconButton(onClick = onAdminPanel) {
                            Icon(Icons.Outlined.SpaceDashboard, contentDescription = "Panel de administración", tint = KarsyNavy, modifier = Modifier.size(18.dp))
                        }
                        HeaderIconButton(onClick = onProfile) {
                            Icon(Icons.Outlined.Person, contentDescription = "Mi perfil", tint = KarsyNavy, modifier = Modifier.size(19.dp))
                        }
                    }
                    else -> {
                        HeaderIconButton(onClick = onFavorites) {
                            HeartIcon(filled = false, size = 17.dp)
                        }
                        HeaderIconButton(onClick = onProfile) {
                            Icon(Icons.Outlined.Person, contentDescription = "Mi perfil", tint = KarsyNavy, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                HeaderIconButton(onClick = onToggleMenu, active = menuOpen) {
                    if (menuOpen) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cerrar filtros", tint = KarsyTeal, modifier = Modifier.size(17.dp))
                    } else {
                        Icon(Icons.Rounded.FilterList, contentDescription = "Filtrar", tint = KarsyNavy, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
        HorizontalDivider(color = KarsyBorder)
    }
}

@Composable
private fun HeaderIconButton(onClick: () -> Unit, active: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(38.dp)
            .clip(shape)
            .background(if (active) KarsyTealLight else KarsyBg)
            .border(1.5.dp, if (active) KarsyTeal else KarsyBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun HeroSection() {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    0f to KarsyNavy,
                    0.6f to KarsyNavyLight,
                    1f to KarsyTeal
                )
            )
            .padding(horizontal = 20.dp, vertical = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "🚗 MARKETPLACE AUTOMOTRIZ #1 EN MÉXICO",
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                letterSpacing = 0.1.em,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                "Encuentra tu próximo\nvehículo ideal",
                fontFamily = Outfit,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyWhite,
                letterSpacing = (-0.02).em,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            Text(
                "Miles de vehículos verificados en toda la república mexicana. Compra, vende y negocia con confianza.",
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            HeroSearch()
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                listOf("12,400+" to "Vehículos", "50+" to "Marcas", "100+" to "Modelos").forEach { (num, lbl) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num, fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KarsyWhite)
                        Text(lbl, fontFamily = DmSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroSearch() {
    var query by rememberSaveable { mutableStateOf("") }
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(1.dp, Color.White.copy(alpha = 0.18f), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = KarsyWhite),
            cursorBrush = SolidColor(KarsyTeal),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text("Buscar marca, modelo o año...", fontFamily = DmSans, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), maxLines = 1)
                    }
                    inner()
                }
            }
        )
        Box(
            Modifier
                .fillMaxHeight()
                .background(KarsyTeal)
                .clickable { /* TODO: buscar publicaciones por marca, modelo o año */ }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Buscar", fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KarsyWhite)
        }
    }
}

@Composable
private fun VisitorBanner(onRegister: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(KarsyTealLight, KarsyBg)))
            .border(1.5.dp, KarsyTeal, shape)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("👀", fontSize = 22.sp)
            Column {
                Text("Estás en modo visitante", fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KarsyNavy)
                Text(
                    "Regístrate para guardar favoritos, contactar vendedores y publicar tu vehículo.",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = KarsyTextSecondary
                )
            }
        }
        Button(
            onClick = onRegister,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 9.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Crear cuenta gratis →", fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: @Composable () -> Unit,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KarsyNavy, letterSpacing = (-0.01).em)
            Text(subtitle, fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid, modifier = Modifier.padding(top = 4.dp))
        }
        action()
    }
}

@Composable
private fun AddFab(
    container: Color,
    content: Color,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .navigationBarsPadding()
            .padding(24.dp)
            .size(56.dp)
            .shadow(if (container == KarsyTeal) 12.dp else 6.dp, CircleShape, spotColor = container)
            .clip(CircleShape)
            .background(container)
            .clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = description, tint = content, modifier = Modifier.size(28.dp))
    }
}
