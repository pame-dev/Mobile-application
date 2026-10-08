package com.pame.karsy.feature.home

import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.alpha
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.EstadoMunicipioPickers
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import com.pame.karsy.core.location.Lugares
import com.pame.karsy.core.location.Lugar
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.Notifications
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
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import com.pame.karsy.core.components.SkeletonCarCard
import com.pame.karsy.core.components.KarsyBrand
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavyDeep
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
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
private val HomeListBg: Color get() = if (Tema.oscuro) KarsyBg else Color(0xFFE9EEF2)

/** Pantalla principal del marketplace (WebHomeScreen del mockup). */
@Composable
fun HomeScreen(
    userMode: UserMode,
    onCarClick: (Long) -> Unit,
    onNotifications: () -> Unit,
    onAdminPanel: () -> Unit,
    onRegister: () -> Unit,
    onLogin: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }
    LightStatusBarIcons()
    // Encabezado y banner comparten un solo degradado diagonal; cada uno pinta su parte.
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var heroHeightPx by remember { mutableFloatStateOf(0f) }
    var eligiendoLugar by rememberSaveable { mutableStateOf(false) }
    if (eligiendoLugar) {
        ElegirLugarSheet(
            onElegir = { vm.cercaDe = it; eligiendoLugar = false },
            onDismiss = { eligiendoLugar = false }
        )
    }
    // El panel de filtros cubre la pantalla: la barra inferior se oculta mientras está abierto.
    HideBottomBarWhile(menuOpen)
    val bottomBarSpace = LocalBottomBarSpace.current

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
    val favoritesToast = stringResource(R.string.home_toast_register_favorites)
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
                onNotifications = onNotifications,
                onAdminPanel = onAdminPanel,
                heroHeightPx = heroHeightPx,
                onHeight = { headerHeightPx = it }
            )

            KarsyPullToRefresh(onRefresh = { done -> vm.refresh(done) }, modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Adaptive(minSize = 280.dp),
                    contentPadding = PaddingValues(bottom = bottomBarSpace + 28.dp),
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
                            onSearch = showResults,
                            headerHeightPx = headerHeightPx,
                            onHeight = { heroHeightPx = it }
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
                            // Carrusel infinito: la lista se repite LOOP_ITEMS veces y empieza a la mitad,
                            // así después de la última viene la primera sin regresar (y se puede deslizar
                            // hacia ambos lados). Con un solo destacado no hay carrusel.
                            val loop = featured.size > 1
                            val featuredState = remember(featured.size) {
                                val start = if (loop) LOOP_ITEMS / 2 - (LOOP_ITEMS / 2) % featured.size else 0
                                LazyListState(firstVisibleItemIndex = start)
                            }
                            if (loop) AutoScroll(featuredState)
                            // Tarjeta centrada: deja a cada lado un margen igual donde asoman la
                            // anterior y la siguiente ([CAROUSEL_PEEK] + [CAROUSEL_GAP]).
                            BoxWithConstraints(Modifier.fillMaxWidth()) {
                                val side = if (loop) CAROUSEL_PEEK + CAROUSEL_GAP else 16.dp
                                val cardWidth = maxWidth - side * 2
                                LazyRow(
                                    state = featuredState,
                                    // Al soltar, la tarjeta más cercana se acomoda al centro.
                                    flingBehavior = rememberSnapFlingBehavior(featuredState, SnapPosition.Center),
                                    contentPadding = PaddingValues(start = side, end = side, top = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(CAROUSEL_GAP)
                                ) {
                                    items(if (loop) LOOP_ITEMS else featured.size) { index ->
                                        val car = featured[index % featured.size]
                                        FeaturedCarCard(
                                            car = car,
                                            isFavorite = car.id in favoriteIds,
                                            onClick = { onCarClick(car.id) },
                                            onToggleFavorite = { toggleFavorite(car.id) },
                                            width = cardWidth
                                        )
                                    }
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
                                container = KarsySurface,
                                textColor = KarsyTextSecondary,
                                label = filterLabel
                            )
                        }
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NearMeChip(
                            lugar = vm.cercaDe,
                            buscando = vm.buscandoUbicacion,
                            onActivar = { vm.activarCercaDeMi(elegirLugar = { eligiendoLugar = true }) },
                            onQuitar = { vm.cercaDe = null },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    when {
                        // Mientras carga: tarjetas de esqueleto con la forma de las publicaciones.
                        vm.loading && allCars.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                repeat(3) { SkeletonCarCard() }
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
                                onAction = { vm.filters = HomeFilters(); vm.query = ""; vm.cercaDe = null }
                            )
                        }
                    }
                    items(allCars, key = { it.id }) { car ->
                        VehicleCard(
                            car = car,
                            isFavorite = car.id in favoriteIds,
                            onClick = { onCarClick(car.id) },
                            onToggleFavorite = { toggleFavorite(car.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        toastMsg?.let { msg ->
            RegisterToast(
                message = msg,
                onClose = { toastMsg = null },
                onRegister = goRegister,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomBarSpace)
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
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KarsyInk),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text(action, fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Encabezado: logo a la izquierda; a la derecha, según el rol,
 * visitante → "Iniciar sesión" + filtros; usuario → avisos + filtros;
 * admin → panel de administración + avisos + filtros.
 * Favoritos y Perfil están en la barra inferior.
 */
@Composable
internal fun HomeHeader(
    userMode: UserMode,
    menuOpen: Boolean,
    onToggleMenu: () -> Unit,
    onLogin: () -> Unit,
    onNotifications: () -> Unit,
    onAdminPanel: () -> Unit,
    /** Alto del banner de abajo, para continuar el mismo degradado. */
    heroHeightPx: Float,
    onHeight: (Float) -> Unit,
) {
    val p = heroPalette()
    // Pinta la parte de arriba del degradado que sigue en el banner: sin línea ni corte entre ambos.
    Column(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height.toFloat()) }
            .drawBehind {
                drawRect(heroBrush(p, width = size.width, top = 0f, totalHeight = size.height + heroHeightPx))
            }
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
            KarsyBrand(color = p.headerContent)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    !userMode.isLoggedIn -> Button(
                        onClick = onLogin,
                        shape = RoundedCornerShape(10.dp),
                        // Turquesa: sobre el azul del encabezado un botón azul no se vería.
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyTeal, contentColor = KarsyWhite),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(stringResource(R.string.home_login), fontFamily = Outfit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    else -> {
                        if (SessionManager.isAdmin) {
                            HeaderIconButton(p, onClick = onAdminPanel) {
                                Icon(Icons.Outlined.SpaceDashboard, contentDescription = stringResource(R.string.home_admin_panel), tint = p.headerContent, modifier = Modifier.size(18.dp))
                            }
                        }
                        HeaderIconButton(p, onClick = onNotifications) {
                            Icon(Icons.Outlined.Notifications, contentDescription = stringResource(R.string.home_notifications), tint = p.headerContent, modifier = Modifier.size(19.dp))
                        }
                    }
                }
                HeaderIconButton(p, onClick = onToggleMenu, active = menuOpen) {
                    if (menuOpen) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.home_close_filters), tint = KarsyTeal, modifier = Modifier.size(17.dp))
                    } else {
                        Icon(Icons.Rounded.FilterList, contentDescription = stringResource(R.string.home_filter), tint = p.headerContent, modifier = Modifier.size(19.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(p: HeroPalette, onClick: () -> Unit, active: Boolean = false, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(38.dp)
            .clip(shape)
            // Botones translúcidos sobre el degradado del encabezado.
            .background(if (active) p.headerButtonActiveBg else p.headerButtonBg)
            .border(1.5.dp, if (active) KarsyTeal else p.headerButtonBorder, shape)
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
    /** Alto del encabezado de arriba: el degradado empieza en él. */
    headerHeightPx: Float,
    onHeight: (Float) -> Unit,
) {
    val p = heroPalette()
    // Bloque recto de borde a borde (sin esquinas redondeadas), pegado al encabezado.
    Box(
        Modifier
            .fillMaxWidth()
            .onSizeChanged { onHeight(it.height.toFloat()) }
            .drawBehind {
                drawRect(heroBrush(p, width = size.width, top = -headerHeightPx, totalHeight = headerHeightPx + size.height))
            }
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = p.tagline, modifier = Modifier.size(15.dp))
                Text(
                    stringResource(R.string.home_hero_tagline),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = p.tagline,
                    letterSpacing = 0.1.em,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                stringResource(R.string.home_hero_title),
                fontFamily = Outfit,
                fontSize = 32.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Bold,
                color = p.title,
                letterSpacing = (-0.02).em,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 14.dp)
            )
            Text(
                stringResource(R.string.home_hero_subtitle),
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                color = p.subtitle,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            HeroSearch(query = query, onQueryChange = onQueryChange, onSearch = onSearch, p = p)
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                modifier = Modifier.padding(top = 24.dp)
            ) {
                stats.forEach { (num, lbl) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Números en turquesa en ambos temas (identidad de la marca).
                        Text(num, fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.statNumber)
                        Text(lbl, fontFamily = DmSans, fontSize = 12.sp, color = p.statLabel)
                    }
                }
            }
        }
    }
}

/**
 * Degradado diagonal de encabezado + banner como si fueran una sola pieza: va de la esquina
 * superior izquierda del encabezado a la inferior derecha del banner. [top] es dónde empieza
 * la pieza completa en las coordenadas de quien dibuja (0 en el encabezado, -alto del
 * encabezado en el banner), así ambos pintan el mismo color en la línea donde se unen.
 */
private fun heroBrush(p: HeroPalette, width: Float, top: Float, totalHeight: Float): Brush =
    Brush.linearGradient(
        colors = listOf(p.gradientStart, p.gradientEnd),
        start = Offset(0f, top),
        end = Offset(width, top + totalHeight.coerceAtLeast(1f))
    )

/**
 * El encabezado de Inicio tiene fondo de color en ambos temas: mientras esta pantalla
 * está visible, la hora y los íconos de la barra de estado van en blanco.
 */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        var ctx = view.context
        while (ctx is ContextWrapper && ctx !is Activity) ctx = ctx.baseContext
        val window = (ctx as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val antes = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (controller != null && antes != null) controller.isAppearanceLightStatusBars = antes }
    }
}

/** Colores del encabezado y el banner de Inicio para el tema en uso. */
private data class HeroPalette(
    val gradientStart: Color,
    val gradientEnd: Color,
    val tagline: Color,
    val title: Color,
    val subtitle: Color,
    val statNumber: Color,
    val statLabel: Color,
    val searchBg: Color,
    val searchBorder: Color,
    val searchText: Color,
    val searchHint: Color,
    val searchButton: Color,
    /** Logo, "Karsy" e íconos del encabezado. */
    val headerContent: Color,
    val headerButtonBg: Color,
    val headerButtonActiveBg: Color,
    val headerButtonBorder: Color,
)

/**
 * Claro: azul petróleo profundo con textos blancos, buscador blanco y "Buscar"
 * en turquesa intenso.
 * Oscuro: azul marino a noche oscura, contadores turquesa y buscador oscuro.
 * El encabezado comparte el degradado del banner (ver heroBrush).
 */
private fun heroPalette(): HeroPalette = if (Tema.oscuro) HeroPalette(
    gradientStart = Color(0xFF0D2B45),
    gradientEnd = Color(0xFF081726),
    tagline = KarsyTeal,
    title = Color.White,
    subtitle = Color(0xFF94A3B8),
    statNumber = KarsyTeal,
    statLabel = Color(0xFF94A3B8),
    searchBg = Color(0xFF061523),
    searchBorder = Color(0xFF1E293B),
    searchText = Color.White,
    searchHint = Color(0xFF94A3B8),
    searchButton = KarsyTeal,
    headerContent = Color.White,
    headerButtonBg = Color.White.copy(alpha = 0.10f),
    headerButtonActiveBg = Color.White.copy(alpha = 0.18f),
    headerButtonBorder = Color.White.copy(alpha = 0.22f),
) else HeroPalette(
    // Azul petróleo profundo: punto medio entre el ciano anterior y el azul noche.
    gradientStart = Color(0xFF1D4E5B),
    gradientEnd = Color(0xFF153B46),
    // Azul hielo: el turquesa de la marca se perdería sobre este fondo.
    tagline = Color(0xFFE2F1F8),
    title = Color.White,
    subtitle = Color(0xFFE2F1F8),
    statNumber = Color.White,
    statLabel = Color(0xFFE2F1F8).copy(alpha = 0.85f),
    searchBg = Color.White.copy(alpha = 0.95f),
    searchBorder = Color.White,
    searchText = Color(0xFF0D2B45),
    searchHint = Color(0xFF8E9A8E),
    searchButton = Color(0xFF2B7A8B),
    headerContent = Color.White,
    headerButtonBg = Color.White.copy(alpha = 0.15f),
    headerButtonActiveBg = Color.White.copy(alpha = 0.28f),
    headerButtonBorder = Color.White.copy(alpha = 0.35f),
)

/** Filtra mientras se escribe; "Buscar" (o la tecla del teclado) baja a los resultados. */
@Composable
private fun HeroSearch(query: String, onQueryChange: (String) -> Unit, onSearch: () -> Unit, p: HeroPalette) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(p.searchBg)
            .border(1.dp, p.searchBorder, shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = p.searchText),
            cursorBrush = SolidColor(KarsyTeal),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(stringResource(R.string.home_search_placeholder), fontFamily = DmSans, fontSize = 14.sp, color = p.searchHint, maxLines = 1)
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
                    tint = p.searchHint,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Box(
            Modifier
                .fillMaxHeight()
                .background(p.searchButton)
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
                Text(stringResource(R.string.home_visitor_title), fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
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
            Text(title, fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KarsyInk, letterSpacing = (-0.01).em)
            Text(subtitle, fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid, modifier = Modifier.padding(top = 4.dp))
        }
        action()
    }
}

/** Tiempo que cada destacado se queda quieto para que se pueda leer. */
private const val AUTO_SCROLL_MS = 6_000L

/** Duración del deslizamiento hacia el siguiente destacado. */
private const val SLIDE_MS = 1_200

/** Acelera y frena suave (ease-in-out): sin arranques ni paradas bruscas. */
private val SlideEasing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

/** Cuánto asoman las tarjetas vecinas a cada lado del destacado centrado. */
private val CAROUSEL_PEEK = 22.dp

/** Separación entre tarjetas del carrusel. */
private val CAROUSEL_GAP = 12.dp

/** Posiciones del carrusel infinito (a 6 s por tarjeta alcanza para horas sin llegar al final). */
private const val LOOP_ITEMS = 10_000

/**
 * Avanza el carrusel una tarjeta cada [AUTO_SCROLL_MS], siempre hacia adelante (el
 * carrusel es infinito), con un deslizamiento suave de [SLIDE_MS]. Si el usuario lo
 * desliza, la cuenta empieza de nuevo al soltarlo.
 */
@Composable
private fun AutoScroll(state: LazyListState) {
    // Solo el arrastre del usuario pausa el carrusel; el desplazamiento automático no
    // (si se usara isScrollInProgress, el propio avance cancelaría su animación).
    val dragging by state.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(state, dragging) {
        if (dragging) return@LaunchedEffect
        while (true) {
            delay(AUTO_SCROLL_MS)
            if (!state.canScrollForward) continue
            // La tarjeta mostrada es la más cercana al centro (las vecinas asoman a los lados);
            // se desliza de su centro al de la siguiente para que esta quede centrada igual.
            val info = state.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            val centered = info.visibleItemsInfo
                .minByOrNull { kotlin.math.abs(it.offset + it.size / 2 - viewportCenter) } ?: continue
            val next = info.visibleItemsInfo.firstOrNull { it.index == centered.index + 1 } ?: continue
            state.animateScrollBy(
                value = (next.offset - centered.offset).toFloat(),
                animationSpec = tween(durationMillis = SLIDE_MS, easing = SlideEasing)
            )
        }
    }
}

/**
 * "Cerca de mí": filtra por el estado y municipio del perfil (los mismos datos que se
 * capturan a mano en el registro), sin pedir permiso de ubicación. Sin sesión, o si el
 * perfil no tiene ubicación, se eligen en una hoja. Activo, muestra el lugar y se quita
 * con un toque.
 */
@Composable
private fun NearMeChip(
    lugar: Lugar?,
    buscando: Boolean,
    onActivar: () -> Unit,
    onQuitar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activo = lugar != null
    val shape = RoundedCornerShape(999.dp)
    Row(modifier.fillMaxWidth()) {
        Text(
            when {
                buscando -> stringResource(R.string.home_near_me_searching)
                lugar != null -> stringResource(
                    R.string.home_near_me_active,
                    listOf(lugar.municipio, lugar.estado).filter { it.isNotBlank() }.joinToString(", ")
                )
                else -> stringResource(R.string.home_near_me)
            },
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (activo) KarsyWhite else KarsyInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(shape)
                .background(if (activo) KarsyTeal else KarsySurface)
                .border(1.dp, if (activo) KarsyTeal else KarsyBorder, shape)
                .clickable(enabled = !buscando) { if (activo) onQuitar() else onActivar() }
                .padding(horizontal = 14.dp, vertical = 9.dp)
        )
    }
}

/** Hoja para elegir estado y municipio a mano (visitantes o perfiles sin ubicación). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ElegirLugarSheet(onElegir: (Lugar) -> Unit, onDismiss: () -> Unit) {
    var estado by rememberSaveable { mutableStateOf("") }
    var municipio by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = KarsySurface) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                stringResource(R.string.home_near_me_pick_title),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
            Text(
                stringResource(R.string.home_near_me_pick_subtitle),
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyTextSecondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            EstadoMunicipioPickers(
                estado = estado,
                municipio = municipio,
                onEstado = { estado = it },
                onMunicipio = { municipio = it }
            )
            // El municipio es opcional: solo con el estado ya se filtra.
            PrimaryButton(
                text = stringResource(R.string.home_near_me_apply),
                enabled = estado.isNotBlank(),
                onClick = { onElegir(Lugar(municipio, estado)) },
                modifier = Modifier.alpha(if (estado.isNotBlank()) 1f else 0.5f)
            )
        }
    }
}
