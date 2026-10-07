package com.pame.karsy.feature.lots

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.components.SkeletonBlock
import com.pame.karsy.core.components.SkeletonCarCard
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.navigation.HideBottomBarWhile
import com.pame.karsy.core.navigation.LocalBottomBarSpace
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavyDeep
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.data.model.Lot
import com.pame.karsy.feature.cardetail.openWhatsapp
import com.pame.karsy.feature.home.FilterSheet
import com.pame.karsy.feature.home.HomeFilters
import com.pame.karsy.feature.home.HomeHeader
import com.pame.karsy.feature.home.VehicleCard
import com.pame.karsy.feature.profile.AvatarViewer
import com.pame.karsy.feature.profile.ProfileAvatar
import com.pame.karsy.feature.profile.ZoomableProfileAvatar

// Paleta del módulo de Lotes; en tema oscuro se usan equivalentes con el mismo contraste.
private val LotIce: Color get() = if (Tema.oscuro) Color(0xFF1F3A4F) else Color(0xFFE2F1F8)
private val LotOutline: Color get() = if (Tema.oscuro) KarsyBorder else Color(0xFFE5E9EC)
private val SoftShadow = KarsyNavyDeep.copy(alpha = 0.06f)

// ── Directorio ───────────────────────────────────────────────────────────────

/** Pestaña "Lotes": directorio de lotes y agencias. Al tocar uno se abre su perfil. */
@Composable
fun LotsScreen(
    userMode: UserMode,
    onLotClick: (String) -> Unit,
    onNotifications: () -> Unit,
    onAdminPanel: () -> Unit,
    onLogin: () -> Unit,
    vm: LotsViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    // El botón de filtros del encabezado muestra/oculta el buscador de lotes.
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    val lots = vm.visibleLots

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        HomeHeader(
            userMode = userMode,
            menuOpen = searchOpen,
            onToggleMenu = {
                searchOpen = !searchOpen
                if (!searchOpen) vm.query = ""
            },
            onLogin = onLogin,
            onNotifications = onNotifications,
            onAdminPanel = onAdminPanel
        )
        KarsyPullToRefresh(onRefresh = { done -> vm.load(done) }, modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp + LocalBottomBarSpace.current),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                item { LotsBanner() }
                if (searchOpen) item {
                    SearchField(vm.query, { vm.query = it }, stringResource(R.string.lots_search_placeholder))
                }
                when {
                    vm.loading && vm.lots.isEmpty() -> items(4) {
                        SkeletonBlock(104.dp, shape = RoundedCornerShape(16.dp))
                    }
                    vm.error != null && vm.lots.isEmpty() -> item {
                        EmptyMessage(vm.error!!, Icons.Outlined.Business, action = stringResource(R.string.home_retry), onAction = { vm.load() })
                    }
                    lots.isEmpty() -> item {
                        EmptyMessage(
                            if (vm.lots.isEmpty()) stringResource(R.string.lots_empty) else stringResource(R.string.lots_no_match),
                            Icons.Outlined.Business
                        )
                    }
                }
                items(lots, key = { it.id }) { lot ->
                    LotCard(lot, onClick = { onLotClick(lot.id) })
                }
            }
        }
    }
}

@Composable
private fun LotsBanner() {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(shape)
            .background(KarsySurface)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(LotIce),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Business, contentDescription = null, tint = KarsyInk, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.padding(start = 14.dp)) {
                Text(stringResource(R.string.lots_banner_title), fontFamily = Outfit, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                Text(
                    stringResource(R.string.lots_banner_subtitle),
                    fontFamily = DmSans,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.06.em,
                    color = KarsyTeal
                )
            }
        }
        Text(
            stringResource(R.string.lots_banner_description),
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = KarsyMid,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun LotCard(lot: Lot, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(KarsySurface)
            .border(1.dp, LotOutline, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        ProfileAvatar(
            name = lot.name,
            avatarUrl = lot.logoUrl,
            size = 56.dp,
            initialsSize = 18.sp,
            modifier = Modifier.border(1.dp, LotOutline, CircleShape)
        )
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(
                lot.name,
                fontFamily = Outfit,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${lot.city} · " + pluralStringResource(R.plurals.lots_vehicle_count, lot.vehicleCount, lot.vehicleCount),
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
            if (lot.description.isNotBlank()) Text(
                lot.description,
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = KarsyMid,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

// ── Perfil del lote ──────────────────────────────────────────────────────────

/** Perfil de un lote: datos, botones de contacto e inventario visible. */
@Composable
fun LotProfileScreen(
    lotId: String,
    userMode: UserMode,
    onBack: () -> Unit,
    onCarClick: (Long) -> Unit,
    onRegister: () -> Unit,
    vm: LotProfileViewModel = viewModel(),
) {
    LaunchedEffect(lotId) { vm.load(lotId) }
    val context = LocalContext.current
    var toastMsg by remember { mutableStateOf<String?>(null) }
    // Panel de filtros (el mismo de Inicio) sobre el inventario del lote; tapa la barra inferior.
    var filtersOpen by rememberSaveable { mutableStateOf(false) }
    HideBottomBarWhile(filtersOpen)
    var avatarOpen by rememberSaveable { mutableStateOf(false) }
    val isVisitor = !userMode.isLoggedIn
    val msgRegisterContact = stringResource(R.string.detail_toast_register_contact)
    val msgRegisterFav = stringResource(R.string.home_toast_register_favorites)
    val lot = vm.lot
    val cars = vm.visibleCars
    val bottomBarSpace = LocalBottomBarSpace.current

    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            SubHeader(title = lot?.name.orEmpty(), onBack = onBack)
            KarsyPullToRefresh(onRefresh = { done -> vm.load(lotId, done) }, modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp + bottomBarSpace),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                ) {
                    when {
                        vm.loading && lot == null -> {
                            item { SkeletonBlock(230.dp, shape = RoundedCornerShape(16.dp)) }
                            items(2) { SkeletonCarCard() }
                        }
                        lot == null -> item {
                            EmptyMessage(
                                vm.error ?: stringResource(R.string.lot_not_found),
                                Icons.Outlined.Business,
                                action = stringResource(R.string.home_retry),
                                onAction = { vm.load(lotId) }
                            )
                        }
                        else -> {
                            item {
                                LotIdentityCard(
                                    lot = lot,
                                    onOpenAvatar = { avatarOpen = true },
                                    // Con sesión, sin número registrado: el botón se ve deshabilitado.
                                    canCall = isVisitor || vm.contact?.phone != null,
                                    canWhatsapp = isVisitor || vm.contact?.whatsapp != null,
                                    onCall = {
                                        val phone = vm.contact?.phone
                                        when {
                                            isVisitor -> toastMsg = msgRegisterContact
                                            phone != null -> runCatching {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone.filter { it.isDigit() || it == '+' })))
                                            }
                                        }
                                    },
                                    onWhatsapp = {
                                        val numero = vm.contact?.whatsapp
                                        when {
                                            isVisitor -> toastMsg = msgRegisterContact
                                            numero != null -> openWhatsapp(context, numero, context.getString(R.string.lot_whatsapp_message, lot.name))
                                        }
                                    },
                                    onDirections = {
                                        // Google Maps (o la app de mapas instalada) busca el lote por nombre y dirección.
                                        val destino = listOf(lot.name, lot.address.ifBlank { lot.city }).joinToString(", ")
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destino))))
                                        }
                                    }
                                )
                            }
                            if (vm.cars.isNotEmpty()) item {
                                Column {
                                    Text(
                                        stringResource(R.string.lot_inventory_title),
                                        fontFamily = Outfit,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KarsyInk,
                                        modifier = Modifier.padding(bottom = 10.dp)
                                    )
                                    // Buscador por texto y, a su lado, el botón del panel de filtros.
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        SearchField(
                                            vm.query,
                                            { vm.query = it },
                                            stringResource(R.string.lot_search_placeholder),
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterButton(active = vm.filtersActive, onClick = { filtersOpen = true })
                                    }
                                }
                            }
                            if (cars.isEmpty()) item {
                                EmptyMessage(
                                    if (vm.cars.isEmpty()) stringResource(R.string.lot_empty_inventory) else stringResource(R.string.lot_no_match),
                                    Icons.Outlined.DirectionsCar
                                )
                            }
                            items(cars, key = { it.id }) { car ->
                                VehicleCard(
                                    car = car,
                                    isFavorite = car.id in vm.favoriteIds,
                                    onClick = { onCarClick(car.id) },
                                    onToggleFavorite = {
                                        if (isVisitor) toastMsg = msgRegisterFav
                                        else vm.toggleFavorite(car.id) { toastMsg = it }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        val logo = lot?.logoUrl
        if (avatarOpen && logo != null) {
            AvatarViewer(name = lot.name, avatarUrl = logo, onClose = { avatarOpen = false })
        }

        FilterSheet(
            visible = filtersOpen,
            userMode = userMode,
            filters = vm.filters,
            options = vm.filterOptions,
            onApply = { vm.filters = it },
            onDismiss = { filtersOpen = false },
            onRegister = {
                filtersOpen = false
                onRegister()
            },
            onClear = { vm.filters = HomeFilters() }
        )

        toastMsg?.let { msg ->
            RegisterToast(
                message = msg,
                onClose = { toastMsg = null },
                onRegister = {
                    toastMsg = null
                    onRegister()
                },
                showRegister = isVisitor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomBarSpace)
            )
        }
    }
}

@Composable
private fun LotIdentityCard(
    lot: Lot,
    onOpenAvatar: () -> Unit,
    canCall: Boolean,
    canWhatsapp: Boolean,
    onCall: () -> Unit,
    onWhatsapp: () -> Unit,
    onDirections: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, ambientColor = SoftShadow, spotColor = SoftShadow)
            .clip(shape)
            .background(KarsySurface)
            .border(1.dp, LotOutline, shape)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZoomableProfileAvatar(
                name = lot.name,
                avatarUrl = lot.logoUrl,
                size = 68.dp,
                initialsSize = 22.sp,
                onOpen = onOpenAvatar,
                modifier = Modifier.border(1.dp, LotOutline, CircleShape)
            )
            Column(Modifier.padding(start = 16.dp)) {
                Text(lot.name, fontFamily = Outfit, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(15.dp))
                    Text(lot.city, fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid, modifier = Modifier.padding(start = 4.dp))
                }
                Text(
                    pluralStringResource(R.plurals.home_vehicles_available, lot.vehicleCount, lot.vehicleCount),
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        if (lot.description.isNotBlank()) Text(
            lot.description,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = KarsyMid,
            modifier = Modifier.padding(top = 14.dp)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            ActionPill(Icons.Outlined.Phone, stringResource(R.string.lot_call), canCall, onCall, Modifier.weight(1f))
            ActionPill(Icons.AutoMirrored.Outlined.Chat, stringResource(R.string.lot_whatsapp), canWhatsapp, onWhatsapp, Modifier.weight(1f))
            ActionPill(Icons.Outlined.Directions, stringResource(R.string.lot_directions), true, onDirections, Modifier.weight(1.2f))
        }
    }
}

@Composable
private fun ActionPill(icon: ImageVector, text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(50))
            .background(LotIce)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.4f)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = KarsyInk, modifier = Modifier.size(16.dp))
        Text(
            text,
            fontFamily = DmSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

// ── Comunes ──────────────────────────────────────────────────────────────────

@Composable
private fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(shape)
            .background(KarsySurface)
            .border(1.dp, LotOutline, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(18.dp))
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 14.sp, color = KarsyInk),
            cursorBrush = SolidColor(KarsyTeal),
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) Text(placeholder, fontFamily = DmSans, fontSize = 14.sp, color = KarsyMid, maxLines = 1)
                    inner()
                }
            }
        )
    }
}

/** Botón cuadrado de filtros junto al buscador; turquesa cuando hay filtros aplicados. */
@Composable
private fun FilterButton(active: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(46.dp)
            .clip(shape)
            .background(if (active) LotIce else KarsySurface)
            .border(1.dp, if (active) KarsyTeal else LotOutline, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.FilterList,
            contentDescription = stringResource(R.string.home_filter),
            tint = if (active) KarsyTeal else KarsyInk,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** Estado vacío / error con ícono ilustrativo. */
@Composable
private fun EmptyMessage(text: String, icon: ImageVector, action: String? = null, onAction: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(LotIce),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = KarsyInk, modifier = Modifier.size(30.dp))
        }
        Text(
            text,
            fontFamily = DmSans,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = KarsyMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp)
        )
        if (action != null) OutlinedButton(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) {
            Text(action, fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = KarsyInk)
        }
    }
}
