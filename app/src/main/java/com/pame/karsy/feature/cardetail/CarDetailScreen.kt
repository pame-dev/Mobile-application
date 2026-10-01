package com.pame.karsy.feature.cardetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.components.HeartIcon
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.StarBadge
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.feature.profile.ProfileAvatar

@Composable
fun CarDetailScreen(
    carId: Long,
    userMode: UserMode,
    onBack: () -> Unit,
    onRegister: () -> Unit,
    onCarClick: (Long) -> Unit,
    /** Contenido extra arriba de la galería (p. ej. el reporte que revisa el admin). */
    topNotice: (@Composable () -> Unit)? = null,
    /** Reemplaza la barra de contacto (p. ej. las acciones del admin al revisar un reporte). */
    bottomBar: (@Composable () -> Unit)? = null,
    vm: CarDetailViewModel = viewModel(),
) {
    LaunchedEffect(carId) { vm.load(carId) }

    val detail = vm.detail
    if (detail == null) {
        when {
            vm.loading -> Box(Modifier.fillMaxSize().background(KarsyBg), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KarsyTeal)
            }
            else -> NotFound(onBack, message = vm.error, onRetry = if (vm.error != null) vm::retry else null)
        }
        return
    }

    val isVisitor = userMode == UserMode.VISITANTE
    val isOwner = detail.sellerId == SessionManager.userId

    var activeImg by rememberSaveable { mutableIntStateOf(0) }
    var showContact by rememberSaveable { mutableStateOf(false) }
    var showSellerProfile by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var showViewer by rememberSaveable { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val msgRegisterFav = stringResource(R.string.detail_toast_register_fav)
    val msgRegisterContact = stringResource(R.string.detail_toast_register_contact)
    val msgRegisterReport = stringResource(R.string.detail_toast_register_report)

    fun handleFav() {
        if (isVisitor) {
            toastMessage = msgRegisterFav
            return
        }
        vm.toggleFavorite { toastMessage = it }
    }

    fun handleContact() {
        if (isVisitor) {
            toastMessage = msgRegisterContact
            return
        }
        vm.loadContact()
        showContact = true
    }

    fun openSeller() {
        vm.loadSellerCars()
        showSellerProfile = true
    }

    fun openReport() {
        if (isVisitor) {
            toastMessage = msgRegisterReport
            return
        }
        showReport = true
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(Modifier.fillMaxSize()) {
            DetailHeader(
                showActions = userMode != UserMode.ADMIN && !isOwner,
                fav = vm.favorite,
                onBack = onBack,
                onFav = ::handleFav,
                onReport = ::openReport,
            )

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {
                topNotice?.invoke()
                // El dueño ve por qué administración deshabilitó su publicación.
                if (isOwner && detail.car.status == "Deshabilitado") {
                    DisabledNotice(reason = detail.car.disabledReason)
                }
                Gallery(
                    detail = detail,
                    activeImg = activeImg,
                    onSelect = { activeImg = it },
                    onOpenFullscreen = { showViewer = true },
                )
                DetailBody(detail = detail, onOpenSeller = ::openSeller)
            }

            // Barra inferior con el botón de contacto (no se muestra en anuncios propios)
            if (bottomBar != null) bottomBar()
            else if (!isOwner) Column(Modifier.background(KarsyBg)) {
                HorizontalDivider(color = KarsyBorder, thickness = 1.dp)
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                ) {
                    PrimaryButton(text = stringResource(R.string.detail_contact_seller), onClick = ::handleContact)
                }
            }
        }

        if (showSellerProfile) {
            SellerProfileView(
                detail = detail,
                contact = vm.contact,
                cars = vm.sellerCars,
                onBack = { showSellerProfile = false },
                onCarClick = { id ->
                    showSellerProfile = false
                    if (id != detail.car.id) onCarClick(id)
                }
            )
        }

        if (showViewer && detail.gallery.isNotEmpty()) {
            ImageViewer(
                images = detail.gallery,
                startIndex = activeImg,
                contentDescription = detail.car.title,
                // Al cerrar, la galería del detalle se queda en la última foto vista.
                onClose = { last ->
                    activeImg = last
                    showViewer = false
                }
            )
        }

        toastMessage?.let { msg ->
            RegisterToast(
                message = msg,
                onClose = { toastMessage = null },
                onRegister = {
                    toastMessage = null
                    onRegister()
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    if (showContact) {
        ContactSheet(
            detail = detail,
            contact = vm.contact,
            loading = vm.contactLoading,
            onDismiss = { showContact = false },
            onOpenProfile = {
                showContact = false
                openSeller()
            },
        )
    }

    if (showReport) {
        ReportSheet(onSubmit = vm::report, onDismiss = { showReport = false })
    }
}

@Composable
private fun DisabledNotice(reason: String?) {
    val danger = Color(0xFFB42318)
    Column(
        Modifier
            .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFFEF3F2))
            .border(1.dp, danger.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Block, contentDescription = null, tint = danger, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.detail_disabled_title),
                fontFamily = Outfit,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = danger
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (reason != null) stringResource(R.string.detail_disabled_reason, reason)
            else stringResource(R.string.detail_disabled_no_reason),
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = KarsyCharcoal
        )
    }
}

@Composable
private fun DetailHeader(
    showActions: Boolean,
    fav: Boolean,
    onBack: () -> Unit,
    onFav: () -> Unit,
    onReport: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(KarsyBg)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = stringResource(R.string.detail_back),
                tint = KarsyNavy,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            stringResource(R.string.detail_title),
            fontFamily = Outfit,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = KarsyNavy,
            modifier = Modifier.align(Alignment.Center)
        )
        if (showActions) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                HeaderSquareButton(onClick = onFav) {
                    HeartIcon(filled = fav, size = 16.dp)
                }
                HeaderSquareButton(onClick = onReport) {
                    Icon(
                        Icons.Rounded.WarningAmber,
                        contentDescription = stringResource(R.string.detail_report_content_desc),
                        tint = KarsyMid,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSquareButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(36.dp)
            .clip(shape)
            .background(KarsyWhite)
            .border(1.5.dp, KarsyBorder, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Gallery(detail: CarDetail, activeImg: Int, onSelect: (Int) -> Unit, onOpenFullscreen: () -> Unit) {
    val gallery = detail.gallery
    val car = detail.car
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFDDE6EC))
            .clickable(enabled = gallery.isNotEmpty(), onClick = onOpenFullscreen)
    ) {
        AsyncImage(
            model = gallery.getOrNull(activeImg),
            contentDescription = car.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        car.badge?.let {
            StarBadge(text = it, modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp))
        }
        if (gallery.isNotEmpty()) Text(
            "${activeImg + 1}/${gallery.size}",
            color = KarsyWhite,
            fontFamily = DmSans,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
    ) {
        // Máximo 6 miniaturas para que quepan en una fila.
        gallery.take(6).forEachIndexed { i, src ->
            val shape = RoundedCornerShape(10.dp)
            AsyncImage(
                model = src,
                contentDescription = stringResource(R.string.detail_photo_n, i + 1),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(shape)
                    .border(2.dp, if (i == activeImg) KarsyNavy else Color.Transparent, shape)
                    .clickable { onSelect(i) }
            )
        }
    }
}

@Composable
private fun DetailBody(detail: CarDetail, onOpenSeller: () -> Unit) {
    val car = detail.car
    val fichaItems = listOf(
        stringResource(R.string.detail_spec_model) to car.model,
        stringResource(R.string.detail_spec_year) to car.year.toString(),
        stringResource(R.string.detail_spec_brand) to car.brand,
        stringResource(R.string.detail_spec_transmission) to detail.transmision,
        stringResource(R.string.detail_spec_mileage) to detail.kilometraje,
        stringResource(R.string.detail_spec_cylinders) to detail.cilindros,
        stringResource(R.string.detail_spec_engine) to detail.motor,
        stringResource(R.string.detail_spec_body_type) to detail.tipoCarro,
        stringResource(R.string.detail_spec_color) to detail.color,
        stringResource(R.string.detail_spec_previous_owners) to detail.cantDuenos,
        stringResource(R.string.detail_spec_fuel) to detail.combustible,
        stringResource(R.string.detail_spec_location) to detail.ubicacion.ifEmpty { "—" },
    )

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text(
            "${car.brand} ${car.model} ${car.year}",
            fontFamily = Outfit,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            buildAnnotatedString {
                append(car.price)
                append(" ")
                withStyle(SpanStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = KarsyMid)) {
                    append(car.currency)
                }
            },
            fontFamily = Outfit,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyTeal,
        )
        Spacer(Modifier.height(18.dp))

        SectionLabel(stringResource(R.string.detail_section_specs))
        Column(
            verticalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            fichaItems.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    row.forEach { (label, value) ->
                        FichaCell(label, value, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        SectionLabel(stringResource(R.string.detail_section_description))
        val noDescription = stringResource(R.string.detail_no_description)
        TextCard(detail.descripcionLarga.ifBlank { noDescription })

        SectionLabel(stringResource(R.string.detail_section_car_details))
        TextCard(detail.detalles)

        SectionLabel(stringResource(R.string.detail_section_seller_contact))
        val shape = RoundedCornerShape(14.dp)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(KarsyWhite)
                .border(1.dp, KarsyBorder, shape)
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            ProfileAvatar(
                name = detail.contactoNombre,
                avatarUrl = detail.sellerAvatar,
                size = 48.dp,
                initialsSize = 17.sp,
                modifier = Modifier
                    .border(2.dp, KarsyBorder, CircleShape)
                    .clickable(onClick = onOpenSeller)
            )
            Column(Modifier.weight(1f)) {
                Text(
                    detail.contactoNombre,
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy
                )
                Text(
                    if (detail.sellerType == "lote") stringResource(R.string.detail_dealer_view_profile) else stringResource(R.string.detail_private_view_profile),
                    fontFamily = DmSans,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .clickable(onClick = onOpenSeller)
                )
            }
        }
    }
}

@Composable
private fun FichaCell(label: String, value: String, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(KarsyWhite)
            .border(1.dp, KarsyBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(label, fontFamily = DmSans, fontSize = 11.sp, color = KarsyMid)
        Text(
            value,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun TextCard(text: String) {
    val shape = RoundedCornerShape(14.dp)
    Text(
        text,
        fontFamily = DmSans,
        fontSize = 13.sp,
        lineHeight = 21.sp,
        color = KarsyCharcoal,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .clip(shape)
            .background(KarsyWhite)
            .border(1.dp, KarsyBorder, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
private fun NotFound(onBack: () -> Unit, message: String? = null, onRetry: (() -> Unit)? = null) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = stringResource(R.string.detail_back), tint = KarsyNavy, modifier = Modifier.size(28.dp))
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (message == null) stringResource(R.string.detail_not_found) else stringResource(R.string.detail_load_error),
                    fontFamily = Outfit,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy
                )
                message?.let {
                    Text(it, fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(Modifier.height(20.dp))
                if (onRetry != null) {
                    PrimaryButton(text = stringResource(R.string.detail_retry), onClick = onRetry)
                    Spacer(Modifier.height(10.dp))
                }
                PrimaryButton(text = stringResource(R.string.detail_back), onClick = onBack)
            }
        }
    }
}
