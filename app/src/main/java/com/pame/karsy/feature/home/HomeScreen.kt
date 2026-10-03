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
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.HeartIcon
import com.pame.karsy.core.components.KarsyBrand
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.session.SessionManager
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

/** Fondo de la lista de publicaciones: un poco más oscuro que KarsyBg para que resalten las tarjetas blancas. */
private val HomeListBg = Color(0xFFE9EEF2)

/** Pantalla principal del marketplace (WebHomeScreen del mockup). */
@Composable
fun HomeScreen(
    userMode: UserMode,
    onCarClick: (Long) -> Unit,
    onFavorites: () -> Unit,
    onProfile: () -> Unit,
    onAdminPanel: () -> Unit,
    onPublish: () -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    onLogout: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var accountMenuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    // Recarga anuncios y favoritos cada vez que se vuelve a esta pantalla.
    LaunchedEffect(Unit) { vm.refresh() }

    val featured = vm.featured
    val allCars = vm.visibleCars
    val searching = vm.query.isNotBlank()
    val gridState = rememberLazyGridState()
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    // Al presionar "Buscar" baja a los resultados (después del encabezado y el aviso de visitante).
    val showResults = {
        keyboard?.hide()
        scope.launch { gridState.animateScrollToItem(if (!userMode.isLoggedIn) 2 else 1) }
        Unit
    }
    val favoriteIds = vm.favoriteIds

    val isVisitor = !userMode.isLoggedIn
    val showHeart = !userMode.isAdmin
    val favoritesToast = stringResource(R.string.home_toast_register_favorites)
    val publishToast = stringResource(R.string.home_toast_register_publish)
    val filterLabel = rememberFilterLabel()

    val toggleFavorite: (Long) -> Unit = { id ->
        if (isVisitor) {
            toastMsg = favoritesToast
        } else {
            vm.toggleFavorite(id) { toastMsg = it }
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
            .background(HomeListBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            HomeHeader(
                userMode = userMode,
                menuOpen = menuOpen,
                onToggleMenu = { menuOpen = !menuOpen },
                onLogin = {
                    toastMsg = null
                    menuOpen = false
                    onLogin()
                },
                onFavorites = onFavorites,
                onAdminPanel = onAdminPanel,
                accountMenu = {
                    val account = SessionManager.account
                    AccountMenu(
                        expanded = accountMenuOpen,
                        name = account?.nombre.orEmpty(),
                        email = account?.correo.orEmpty(),
                        avatarUrl = vm.avatarUrl,
                        onDismiss = { accountMenuOpen = false },
                        onProfile = onProfile,
                        onLogout = { confirmLogout = true }
                    )
                },
                accountMenuOpen = accountMenuOpen,
                onToggleAccountMenu = { accountMenuOpen = !accountMenuOpen }
            )

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 280.dp),
                contentPadding = PaddingValues(bottom = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    HeroSection(
                        stats = vm.heroStats,
                        query = vm.query,
                        onQueryChange = { vm.query = it },
                        onSearch = showResults
                    )
                }

                if (isVisitor) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        VisitorBanner(onRegister = goRegister, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp))
                    }
                }

                // Vehículos destacados (solicitudes de destacado aprobadas y vigentes)
                // Mientras se busca se ocultan para que los resultados queden arriba.
                if (featured.isNotEmpty() && !searching) item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(top = 8.dp)) {
                        SectionHeader(
                            title = stringResource(R.string.home_featured_title),
                            subtitle = stringResource(R.string.home_featured_subtitle),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {}
                        val featuredState = rememberLazyListState()
                        AutoScroll(featuredState, itemCount = featured.size)
                        LazyRow(
                            state = featuredState,
                            flingBehavior = rememberSnapFlingBehavior(featuredState),
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
                        title = stringResource(R.string.home_all_title),
                        subtitle = pluralStringResource(R.plurals.home_vehicles_available, allCars.size, allCars.size),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        KarsySelect(
                            selected = vm.filters.orden,
                            options = ORDEN_OPCIONES,
                            onSelect = { vm.filters = vm.filters.copy(orden = it) },
                            fillWidth = false,
                            container = KarsyWhite,
                            textColor = KarsyTextSecondary,
                            label = filterLabel
                        )
                    }
                }
                when {
                    vm.loading && allCars.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = KarsyTeal)
                        }
                    }
                    vm.error != null && allCars.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyMessage(vm.error!!, action = stringResource(R.string.home_retry), onAction = vm::refresh)
                    }
                    allCars.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyMessage(
                            if (vm.cars.isEmpty()) stringResource(R.string.home_empty_no_vehicles)
                            else stringResource(R.string.home_empty_no_match),
                            action = if (vm.cars.isEmpty()) null else stringResource(R.string.home_clear_filters),
                            onAction = { vm.filters = HomeFilters(); vm.query = "" }
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

        // Botón flotante "+" (particulares, lotes y administradores pueden publicar)
        when {
            isVisitor -> AddFab(
                container = KarsyDisabled,
                content = KarsyMid,
                description = stringResource(R.string.home_fab_login_to_add),
                onClick = { toastMsg = publishToast },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            else -> AddFab(
                container = KarsyTeal,
                content = KarsyWhite,
                description = stringResource(R.string.home_fab_add_vehicle),
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
            filters = vm.filters,
            options = vm.filterOptions,
            onApply = { vm.filters = it },
            onDismiss = { menuOpen = false },
            onRegister = goRegister
        )

        LogoutConfirmDialog(
            visible = confirmLogout,
            onCancel = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                onLogout()
            }
        )
    }
}

@Composable
private fun EmptyMessage(text: String, action: String?, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, fontFamily = DmSans, fontSize = 14.sp, color = KarsyMid, textAlign = TextAlign.Center)
        if (action != null) {
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.5.dp, KarsyBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KarsyNavy),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(action, fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun HomeHeader(
    userMode: UserMode,
    menuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onLogin: () -> Unit,
    onFavorites: () -> Unit,
    onAdminPanel: () -> Unit,
    accountMenuOpen: Boolean,
    onToggleAccountMenu: () -> Unit,
    accountMenu: @Composable () -> Unit,
) {
    // Ícono de perfil: abre/cierra el menú de la cuenta, que se ancla justo debajo.
    val profileButton = @Composable {
        Box {
            HeaderIconButton(onClick = onToggleAccountMenu, active = accountMenuOpen) {
                Icon(Icons.Outlined.Person, contentDescription = stringResource(R.string.home_my_profile), tint = if (accountMenuOpen) KarsyTeal else KarsyNavy, modifier = Modifier.size(19.dp))
            }
            accountMenu()
        }
    }
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
                        onClick = onLogin,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(38.dp)
                            .shadow(4.dp, RoundedCornerShape(10.dp), spotColor = KarsyNavy.copy(alpha = 0.3f))
                    ) {
                        Text(stringResource(R.string.home_login), fontFamily = Outfit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    userMode.isAdmin -> {
                        HeaderIconButton(onClick = onAdminPanel) {
                            Icon(Icons.Outlined.SpaceDashboard, contentDescription = stringResource(R.string.home_admin_panel), tint = KarsyNavy, modifier = Modifier.size(18.dp))
                        }
                        profileButton()
                    }
                    else -> {
                        HeaderIconButton(onClick = onFavorites) {
                            HeartIcon(filled = false, size = 17.dp)
                        }
                        profileButton()
                    }
                }
                HeaderIconButton(onClick = onToggleMenu, active = menuOpen) {
                    if (menuOpen) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.home_close_filters), tint = KarsyTeal, modifier = Modifier.size(17.dp))
                    } else {
                        Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.home_filter), tint = KarsyNavy, modifier = Modifier.size(19.dp))
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
private fun HeroSection(
    stats: List<Pair<String, String>>,
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
) {
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
                stringResource(R.string.home_hero_tagline),
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                letterSpacing = 0.1.em,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                stringResource(R.string.home_hero_title),
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
                stringResource(R.string.home_hero_subtitle),
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            HeroSearch(query = query, onQueryChange = onQueryChange, onSearch = onSearch)
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                stats.forEach { (num, lbl) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(num, fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KarsyWhite)
                        Text(lbl, fontFamily = DmSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
}

/** Filtra mientras se escribe; "Buscar" (o la tecla del teclado) baja a los resultados. */
@Composable
private fun HeroSearch(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit) {
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
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = KarsyWhite),
            cursorBrush = SolidColor(KarsyTeal),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.home_search_placeholder), fontFamily = DmSans, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), maxLines = 1)
                    }
                    inner()
                }
            }
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.home_search_clear),
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxHeight()
                .background(KarsyTeal)
                .clickable(onClick = onSearch)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.home_search), fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KarsyWhite)
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
                Text(stringResource(R.string.home_visitor_title), fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KarsyNavy)
                Text(
                    stringResource(R.string.home_visitor_message),
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
            Text(stringResource(R.string.home_create_account_free), fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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

/** Intervalo entre cambios automáticos del carrusel de destacados. */
private const val AUTO_SCROLL_MS = 4_000L

/**
 * Avanza el carrusel una tarjeta cada [AUTO_SCROLL_MS] y al llegar al final vuelve
 * al inicio. Si el usuario lo desliza, la cuenta empieza de nuevo al soltarlo.
 */
@Composable
private fun AutoScroll(state: LazyListState, itemCount: Int) {
    if (itemCount < 2) return
    // Solo el arrastre del usuario pausa el carrusel; el desplazamiento automático no
    // (si se usara isScrollInProgress, el propio avance cancelaría su animación).
    val dragging by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(state, itemCount, dragging) {
        if (dragging) return@LaunchedEffect
        while (true) {
            delay(AUTO_SCROLL_MS)
            val next = if (state.canScrollForward) state.firstVisibleItemIndex + 1 else 0
            state.animateScrollToItem(next.coerceAtMost(itemCount - 1))
        }
    }
}
