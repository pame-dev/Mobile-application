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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.HeartIcon
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.RegisterToast
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.StarBadge
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
import com.pame.karsy.data.repository.CarRepository

/** Foto de ejemplo del vendedor (la misma que usa el mockup). */
internal fun sellerAvatar(size: Int) =
    "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=$size&h=$size&fit=crop&auto=format"

@Composable
fun CarDetailScreen(carId: Int, userMode: UserMode, onBack: () -> Unit, onRegister: () -> Unit) {
    // TODO: cargar desde Supabase (publicaciones + fotos_publicacion + datos de contacto) en CarDetailViewModel.
    val detail = remember(carId) { CarRepository.getDetail(carId) }
    if (detail == null) {
        NotFound(onBack)
        return
    }

    val isVisitor = userMode == UserMode.VISITANTE

    // TODO: leer el estado inicial desde public.favoritos del usuario en sesión.
    var fav by rememberSaveable { mutableStateOf(false) }
    var activeImg by rememberSaveable { mutableIntStateOf(0) }
    var showContact by rememberSaveable { mutableStateOf(false) }
    var showSellerProfile by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    fun handleFav() {
        if (isVisitor) {
            toastMessage = "Regístrate para guardar tus favoritos"
            return
        }
        // TODO: insertar/eliminar en public.favoritos (id_cuenta, id_publicacion).
        fav = !fav
    }

    fun handleContact() {
        if (isVisitor) {
            toastMessage = "Regístrate para contactar al vendedor"
            return
        }
        // TODO: registrar interacciones_publicacion tipo 'contactar'.
        showContact = true
    }

    fun openReport() {
        if (isVisitor) {
            toastMessage = "Regístrate para realizar un reporte"
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
                showActions = userMode != UserMode.ADMIN,
                fav = fav,
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
                Gallery(
                    detail = detail,
                    activeImg = activeImg,
                    onSelect = { activeImg = it },
                )
                DetailBody(detail = detail, onOpenSeller = { showSellerProfile = true })
            }

            // Barra inferior con el botón de contacto
            Column(Modifier.background(KarsyBg)) {
                HorizontalDivider(color = KarsyBorder, thickness = 1.dp)
                Box(
                    Modifier
                        .navigationBarsPadding()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
                ) {
                    PrimaryButton(text = "Contactar vendedor", onClick = ::handleContact)
                }
            }
        }

        if (showSellerProfile) {
            SellerProfileView(detail = detail, onBack = { showSellerProfile = false })
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
            onDismiss = { showContact = false },
            onOpenProfile = {
                showContact = false
                showSellerProfile = true
            },
        )
    }

    if (showReport) {
        ReportSheet(onDismiss = { showReport = false })
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
                contentDescription = "Regresar",
                tint = KarsyNavy,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            "Detalle del vehículo",
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
                        contentDescription = "Reportar publicación o cuenta",
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
private fun Gallery(detail: CarDetail, activeImg: Int, onSelect: (Int) -> Unit) {
    val gallery = detail.gallery
    val car = detail.car
    Box(
        Modifier
            .fillMaxWidth()
            .height(300.dp)
            .background(Color(0xFFDDE6EC))
    ) {
        AsyncImage(
            model = gallery[activeImg],
            contentDescription = car.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        car.badge?.let {
            StarBadge(text = it, modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp))
        }
        Text(
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
        gallery.forEachIndexed { i, src ->
            val shape = RoundedCornerShape(10.dp)
            AsyncImage(
                model = src,
                contentDescription = "Foto ${i + 1}",
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
        "Modelo" to car.model,
        "Año" to car.year.toString(),
        "Marca" to car.brand,
        "Transmisión" to detail.transmision,
        "Kilometraje" to detail.kilometraje,
        "Cilindros" to detail.cilindros,
        "Caballos de fuerza" to detail.caballos,
        "Tipo de carro" to detail.tipoCarro,
        "Color" to detail.color,
        "Cant. de dueños" to detail.cantDuenos,
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
                    append("MXN")
                }
            },
            fontFamily = Outfit,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyTeal,
        )
        Spacer(Modifier.height(18.dp))

        SectionLabel("Ficha técnica")
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

        SectionLabel("Descripción")
        TextCard(detail.descripcionLarga)

        SectionLabel("Detalles del auto")
        TextCard(detail.detalles)

        SectionLabel("Contacto del vendedor")
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
            AsyncImage(
                model = sellerAvatar(88),
                contentDescription = "Ver perfil de ${detail.contactoNombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
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
                    "Ver perfil",
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
private fun NotFound(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(onClick = onBack, modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Regresar", tint = KarsyNavy, modifier = Modifier.size(28.dp))
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
                    "Vehículo no encontrado",
                    fontFamily = Outfit,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy
                )
                Spacer(Modifier.height(20.dp))
                PrimaryButton(text = "Regresar", onClick = onBack)
            }
        }
    }
}
