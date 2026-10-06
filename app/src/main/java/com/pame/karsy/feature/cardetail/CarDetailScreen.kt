package com.pame.karsy.feature.cardetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
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
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car
import com.pame.karsy.feature.dashboard.DestacarDialog
import com.pame.karsy.feature.dashboard.SolicitudEnviadaDialog
import com.pame.karsy.data.model.CarDetail
import com.pame.karsy.feature.profile.ProfileAvatar
import kotlinx.coroutines.launch

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
    /** Abre el formulario para corregir y reenviar una publicación rechazada propia. */
    onEditPublication: (Long) -> Unit = {},
    vm: CarDetailViewModel = viewModel(),
) {
    // Al volver de corregirla se recarga para mostrar que ya está en revisión.
    var returningFromEdit by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(carId) {
        if (returningFromEdit) {
            returningFromEdit = false
            vm.retry()
        } else vm.load(carId)
    }

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
    var confirmPause by rememberSaveable { mutableStateOf(false) }
    var askFeature by rememberSaveable { mutableStateOf(false) }
    var featureSent by rememberSaveable { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val msgPaused = stringResource(R.string.detail_owner_paused)
    val msgResumed = stringResource(R.string.detail_owner_resumed)

    fun setPaused(paused: Boolean) {
        vm.setPaused(paused) { error ->
            confirmPause = false
            toastMessage = error ?: if (paused) msgPaused else msgResumed
        }
    }

    fun editPublication() {
        returningFromEdit = true
        onEditPublication(detail.car.id)
    }
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
                // Rechazada (o rechazados sus cambios): el dueño ve el motivo y puede corregirla.
                if (isOwner && detail.car.rejectedReason != null) {
                    DisabledNotice(
                        reason = detail.car.rejectedReason,
                        title = stringResource(
                            if (detail.car.approved) R.string.detail_changes_rejected_title else R.string.detail_rejected_title
                        ),
                        actionLabel = stringResource(R.string.profile_dashboard_fix_resubmit),
                        onAction = ::editPublication
                    )
                }
                Gallery(
                    detail = detail,
                    activeImg = activeImg,
                    onSelect = { activeImg = it },
                    onOpenFullscreen = { showViewer = true },
                )
                DetailBody(detail = detail, onOpenSeller = ::openSeller)
            }

            // Barra inferior: contacto para compradores; editar / deshabilitar para el dueño.
            if (bottomBar != null) bottomBar()
            else if (isOwner) OwnerActionsBar(
                car = detail.car,
                updating = vm.updatingStatus,
                onEdit = ::editPublication,
                onPause = { confirmPause = true },
                onResume = { setPaused(false) },
                onFeature = { askFeature = true },
            )
            else Column(Modifier.background(KarsyBg)) {
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

        if (confirmPause) {
            AlertDialog(
                onDismissRequest = { if (!vm.updatingStatus) confirmPause = false },
                containerColor = KarsyWhite,
                title = { Text(stringResource(R.string.detail_owner_pause_title), fontFamily = Outfit, fontWeight = FontWeight.Bold, color = KarsyNavy) },
                text = { Text(stringResource(R.string.detail_owner_pause_desc), fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal) },
                confirmButton = {
                    TextButton(onClick = { setPaused(true) }, enabled = !vm.updatingStatus) {
                        Text(stringResource(R.string.detail_owner_pause), fontFamily = DmSans, fontWeight = FontWeight.Bold, color = KarsyError)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmPause = false }, enabled = !vm.updatingStatus) {
                        Text(stringResource(R.string.admin1_cancel), fontFamily = DmSans, color = KarsyNavy)
                    }
                }
            )
        }

        // Mismos diálogos que en Mi Panel para pedir que se destaque.
        if (askFeature) {
            DestacarDialog(
                carName = "${detail.car.title} ${detail.car.year}",
                onConfirm = {
                    askFeature = false
                    vm.requestFeatured { error -> if (error == null) featureSent = true else toastMessage = error }
                },
                onCancel = { askFeature = false }
            )
        }
        if (featureSent) {
            SolicitudEnviadaDialog(carName = "${detail.car.title} ${detail.car.year}", onClose = { featureSent = false })
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

/**
 * Acciones del dueño sobre su publicación: deshabilitarla / habilitarla, pedir que se
 * destaque y editarla (los cambios pasan por revisión; mientras tanto no se edita).
 */
@Composable
private fun OwnerActionsBar(
    car: Car,
    updating: Boolean,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFeature: () -> Unit,
) {
    // Pausar y destacar solo aplican a una publicación aprobada que el dueño controla.
    val canToggle = car.approved && car.status in listOf("Activo", "Pausado")
    val showFeature = car.approved && car.status == "Activo"
    // Una rechazada se corrige desde el aviso de arriba.
    val canEdit = car.status != "Vendido" && car.rejectedReason == null
    if (!canToggle && !showFeature && !canEdit) return

    Column(Modifier.background(KarsyBg)) {
        HorizontalDivider(color = KarsyBorder, thickness = 1.dp)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)
        ) {
            if (canToggle) {
                val paused = car.status == "Pausado"
                OwnerButton(
                    text = stringResource(if (paused) R.string.detail_owner_resume else R.string.detail_owner_pause),
                    onClick = if (paused) onResume else onPause,
                    enabled = !updating,
                    filled = paused,
                    color = if (paused) KarsyTeal else KarsyError,
                    modifier = Modifier.weight(1f)
                )
            }
            if (showFeature) OwnerButton(
                text = when {
                    car.featured && car.featuredUntil != null -> stringResource(R.string.profile_dashboard_featured_until, car.featuredUntil)
                    car.featured -> stringResource(R.string.profile_dashboard_featured)
                    car.featuredPending -> stringResource(R.string.profile_dashboard_request_sent)
                    else -> stringResource(R.string.profile_dashboard_feature)
                },
                onClick = onFeature,
                // Una solicitud a la vez.
                enabled = !car.featured && !car.featuredPending,
                color = FeatureGold,
                modifier = Modifier.weight(1f)
            )
            if (canEdit) OwnerButton(
                text = stringResource(if (car.inReview) R.string.detail_owner_in_review else R.string.detail_owner_edit),
                onClick = onEdit,
                enabled = !car.inReview,
                filled = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val FeatureGold = Color(0xFFB7791F)

/** Botón compacto de la barra del dueño: relleno o con borde del color dado. */
@Composable
private fun OwnerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    color: Color = KarsyNavy,
) {
    val shape = RoundedCornerShape(12.dp)
    val content = @Composable {
        Text(
            text,
            fontFamily = Outfit,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    val sizing = modifier.height(40.dp)
    val padding = PaddingValues(horizontal = 6.dp)
    if (filled) Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        contentPadding = padding,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = KarsyWhite,
            disabledContainerColor = KarsyBorder,
            disabledContentColor = KarsyMid,
        ),
        modifier = sizing
    ) { content() }
    else OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        contentPadding = padding,
        border = BorderStroke(1.5.dp, if (enabled) color.copy(alpha = 0.6f) else KarsyBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = KarsyWhite,
            contentColor = color,
            disabledContentColor = KarsyMid,
        ),
        modifier = sizing
    ) { content() }
}

@Composable
private fun DisabledNotice(
    reason: String?,
    title: String = stringResource(R.string.detail_disabled_title),
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
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
                title,
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
        if (actionLabel != null) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KarsyTeal, contentColor = KarsyWhite),
                modifier = Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(42.dp)
            ) {
                Text(actionLabel, fontFamily = Outfit, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
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
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(initialPage = activeImg) { gallery.size }
    // La foto que se ve es la seleccionada; al cerrar el visor se sincroniza al revés.
    LaunchedEffect(pager.currentPage) { onSelect(pager.currentPage) }
    LaunchedEffect(activeImg) { if (pager.currentPage != activeImg) pager.scrollToPage(activeImg) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFDDE6EC))
    ) {
        // Se desliza entre fotos; al tocar se abre a pantalla completa.
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = gallery[page],
                contentDescription = car.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onOpenFullscreen)
            )
        }
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

    // Todas las miniaturas (hasta 15) en una fila que se desplaza.
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)
    ) {
        gallery.forEachIndexed { i, src ->
            val shape = RoundedCornerShape(10.dp)
            AsyncImage(
                model = src,
                contentDescription = stringResource(R.string.detail_photo_n, i + 1),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 64.dp, height = 54.dp)
                    .clip(shape)
                    .border(2.dp, if (i == activeImg) KarsyNavy else Color.Transparent, shape)
                    .clickable { scope.launch { pager.animateScrollToPage(i) } }
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
