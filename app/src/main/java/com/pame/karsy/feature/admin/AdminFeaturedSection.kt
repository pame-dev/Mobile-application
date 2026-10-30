package com.pame.karsy.feature.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.net.toUri
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.theme.KarsyGreen
import com.pame.karsy.core.theme.KarsyGreenSoft
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsyRed
import com.pame.karsy.core.theme.KarsyRedSoft
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyTealText
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.util.PagoDestacado
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.KarsyBackButton
import com.pame.karsy.R
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.core.theme.karsyTint
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsySuccess
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.feature.profile.ProfileAvatar

private val FeaturedYellowBg: Color get() = karsyTint(Color(0xFFD99A1E), Color(0xFFFFF4CC))
private val FeaturedYellowFg: Color get() = if (Tema.oscuro) Color(0xFFFBBF24) else Color(0xFF8A6415)
private val ApprovedBg: Color get() = karsyTint(KarsyGreen, KarsyGreenSoft)
private val RejectedBg: Color get() = karsyTint(KarsyRed, KarsyRedSoft)
private val RejectedFg: Color get() = BadgeTone.Danger.fg

@Composable
fun AdminFeaturedSection(vm: AdminViewModel) {
    val requests = vm.featuredRequests
    var filter by rememberSaveable { mutableStateOf(FeaturedStatus.Pendiente) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showApproved by remember { mutableStateOf(false) }
    var showReject by remember { mutableStateOf(false) }
    var reviewingPayment by remember { mutableStateOf(false) }
    // Se tocó Aprobar o Ver comprobante en "Revisar pago": su error se ve dentro de la ventana.
    var acted by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val selected = requests.firstOrNull { it.id == selectedId }

    if (selected != null) {
        BackHandler { selectedId = null }
        FeaturedRequestDetail(
            request = selected,
            onBackToList = { selectedId = null },
            onReject = { showReject = true },
            onApprove = {
                // admin_resolver_destacado: destaca los días de config_pagos y registra la acción.
                vm.resolveFeatured(selected, approve = true)
                showApproved = true
            },
            // Con comprobante: antes de aprobar se revisa que el pago haya llegado.
            onReviewPayment = {
                acted = false
                vm.dismissMessage()
                reviewingPayment = true
            }
        )

        if (reviewingPayment) {
            RevisarPagoDialog(
                request = selected,
                working = vm.working,
                error = vm.message.takeIf { acted },
                onOpenReceipt = {
                    acted = true
                    vm.dismissMessage()
                    vm.openReceipt(selected) { url ->
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                        } catch (e: ActivityNotFoundException) {
                            vm.message = texto(R.string.admin2_verif_open_error)
                        }
                    }
                },
                onApprove = {
                    acted = true
                    vm.dismissMessage()
                    vm.resolveFeatured(selected, approve = true, onSuccess = {
                        reviewingPayment = false
                        showApproved = true
                    })
                },
                onReject = {
                    reviewingPayment = false
                    vm.dismissMessage()
                    showReject = true
                },
                onDismiss = { if (!vm.working) reviewingPayment = false }
            )
        }

        if (showApproved) {
            ApprovedDialog(
                request = selected,
                onDone = {
                    showApproved = false
                    selectedId = null
                    filter = FeaturedStatus.Aprobada
                }
            )
        }
        if (showReject) {
            RejectDialog(
                onCancel = { showReject = false },
                onConfirm = { reason, comment ->
                    val motivo = listOf(reason, comment.trim()).filter { it.isNotEmpty() }.joinToString(". ")
                    vm.resolveFeatured(selected, approve = false, reason = motivo)
                    showReject = false
                    selectedId = null
                    filter = FeaturedStatus.Rechazada
                }
            )
        }
        return
    }

    val visible = requests.filter { it.status == filter }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin1_featured_title),
            description = stringResource(R.string.admin1_featured_desc)
        )

        // Precio, días y cuenta bancaria: solo el administrador (admin_guardar_config_pagos).
        if (SessionManager.isAdmin) {
            PaymentSettingsCard(vm)
            Spacer(Modifier.height(16.dp))
        }

        AdminCard {
            AdminFilterBar {
                val statusLabels = FeaturedStatus.entries.map { status ->
                    stringResource(R.string.admin1_filter_with_count, featuredFilterLabel(status), requests.count { it.status == status })
                }
                AdminListHeader(
                    title = stringResource(R.string.admin2_featured_list_title),
                    count = pluralStringResource(R.plurals.admin2_featured_count, requests.size, requests.size)
                ) {
                    AdminSelect(
                        value = statusLabels[filter.ordinal],
                        options = statusLabels,
                        onSelect = { label -> statusLabels.indexOf(label).takeIf { it >= 0 }?.let { filter = FeaturedStatus.entries[it] } }
                    )
                }
            }

            // Mientras carga: filas de esqueleto en lugar de "no se encontraron".
            if (vm.loading && visible.isEmpty()) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    repeat(4) { SkeletonListRow(circle = false) }
                }
            } else if (visible.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Outlined.StarOutline,
                    title = stringResource(R.string.admin1_featured_empty),
                    description = stringResource(R.string.admin1_featured_desc)
                )
            } else {
                visible.forEachIndexed { index, request ->
                    AdminListRow(isLast = index == visible.lastIndex) {
                        FeaturedRequestRow(request, onClick = { selectedId = request.id })
                    }
                }
            }
        }
    }
}

@Composable
private fun featuredFilterLabel(status: FeaturedStatus): String = when (status) {
    FeaturedStatus.Pendiente -> stringResource(R.string.admin1_filter_pending)
    FeaturedStatus.Aprobada -> stringResource(R.string.admin1_filter_approved)
    FeaturedStatus.Rechazada -> stringResource(R.string.admin1_filter_rejected)
}

/** Fila con el formato de Reportes: tiempo y estado, miniatura con el auto, solicitante y acción. */
@Composable
private fun FeaturedRequestRow(request: FeaturedRequest, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminBadge(request.time, BadgeTone.Neutral)
        Spacer(Modifier.weight(1f))
        when (request.status) {
            FeaturedStatus.Pendiente -> AdminStatusBadge("Pendiente")
            FeaturedStatus.Aprobada -> AdminBadge(stringResource(R.string.admin1_badge_featured), BadgeTone.Success)
            FeaturedStatus.Rechazada -> AdminBadge(stringResource(R.string.admin1_badge_rejected), BadgeTone.Danger)
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        AdminThumbnail(request.image, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(request.vehicle, fontFamily = DmSans, fontSize = KarsyTextSize.BodyLarge, fontWeight = FontWeight.Bold, color = KarsyInk)
            Spacer(Modifier.height(2.dp))
            Text(
                listOf(request.year.takeIf { it > 0 }?.toString(), request.price).filterNotNull().joinToString(" · "),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                color = AdminColors.TableText
            )
        }
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        AdminField(stringResource(R.string.admin2_requested_by), request.user, Modifier.weight(1f))
        AdminActionButton(stringResource(R.string.admin2_review), onClick = onClick)
    }
    PaymentSummary(request)
}

/** Monto y referencia del pago; las solicitudes viejas sin pago llevan la etiqueta de prueba. */
@Composable
private fun PaymentSummary(request: FeaturedRequest) {
    if (request.amount == null) {
        AdminBadge(stringResource(R.string.admin2_pay_trial), BadgeTone.Warning)
        return
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        AdminField(stringResource(R.string.admin2_pay_amount), request.amount, Modifier.weight(1f))
        AdminField(stringResource(R.string.admin2_pay_reference), request.reference ?: "—", Modifier.weight(1f))
    }
}

@Composable
private fun FeaturedRequestDetail(
    request: FeaturedRequest,
    onBackToList: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit,
    onReviewPayment: () -> Unit,
) {
    val noDescription = stringResource(R.string.admin1_no_description)
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .then(if (request.status != FeaturedStatus.Pendiente) Modifier.navigationBarsPadding() else Modifier)
        ) {
            KarsyBackButton(
                onClick = onBackToList,
                contentDescription = stringResource(R.string.admin1_back_to_requests),
                modifier = Modifier.padding(bottom = 14.dp)
            )
            AdminSectionHeader(
                title = stringResource(R.string.admin1_review_title),
                description = stringResource(R.string.admin1_review_desc)
            )

            val shape = RoundedCornerShape(KarsyRadius.Xl)
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.1f), spotColor = KarsyNavy.copy(alpha = 0.1f))
                    .clip(shape)
                    .background(KarsySurface)
                    .border(1.dp, KarsyBorder, shape)
            ) {
                Box {
                    AsyncImage(
                        model = request.image,
                        contentDescription = request.vehicle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(220.dp).background(KarsyBg)
                    )
                    if (request.status == FeaturedStatus.Aprobada) {
                        Text(
                            stringResource(R.string.admin1_badge_featured),
                            fontFamily = DmSans,
                            fontSize = KarsyTextSize.Small,
                            fontWeight = FontWeight.ExtraBold,
                            color = FeaturedYellowFg,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(CircleShape)
                                .background(FeaturedYellowBg)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(request.year.toString(), fontFamily = DmSans, fontSize = KarsyTextSize.Body, color = KarsyMid)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                request.vehicle,
                                fontFamily = Outfit,
                                fontSize = KarsyTextSize.HeadlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = KarsyInk
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            request.price,
                            fontFamily = Outfit,
                            fontSize = KarsyTextSize.TitleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = KarsyTealText
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    SectionLabel(stringResource(R.string.admin1_specs))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KarsyRadius.Lg))
                            .background(KarsyBg)
                            .padding(16.dp)
                    ) {
                        Row {
                            SpecText(stringResource(R.string.admin1_spec_transmission), request.transmision, Modifier.weight(1f))
                            SpecText(stringResource(R.string.admin1_spec_mileage), request.kilometraje, Modifier.weight(1f))
                        }
                        Row {
                            SpecText(stringResource(R.string.admin1_spec_color), request.color, Modifier.weight(1f))
                            SpecText(stringResource(R.string.admin1_spec_fuel), request.combustible, Modifier.weight(1f))
                        }
                    }

                    SectionLabel(stringResource(R.string.admin1_description))
                    Text(
                        request.description.ifBlank { noDescription },
                        fontFamily = DmSans,
                        fontSize = KarsyTextSize.BodyLarge,
                        lineHeight = 23.sp,
                        color = KarsyTextSecondary
                    )

                    SectionLabel(stringResource(R.string.admin2_pay_section))
                    PaymentSummary(request)

                    SectionLabel(stringResource(R.string.admin1_requester))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KarsyRadius.Lg))
                            .background(KarsySurface)
                            .border(1.dp, KarsyBorder, RoundedCornerShape(KarsyRadius.Lg))
                            .padding(15.dp)
                    ) {
                        ProfileAvatar(
                            name = request.user,
                            avatarUrl = request.userAvatar,
                            size = 54.dp,
                            initialsSize = 18.sp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                request.user,
                                fontFamily = Outfit,
                                fontSize = KarsyTextSize.Title,
                                fontWeight = FontWeight.Bold,
                                color = KarsyInk
                            )
                            Text(
                                stringResource(R.string.admin1_view_profile),
                                fontFamily = DmSans,
                                fontSize = KarsyTextSize.Small,
                                fontWeight = FontWeight.Bold,
                                color = KarsyTealText,
                                // TODO: navegar al perfil del usuario solicitante
                                modifier = Modifier.clickable { }.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        if (request.status == FeaturedStatus.Pendiente) {
            HorizontalDivider(thickness = 1.dp, color = KarsyBorder)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KarsySurface.copy(alpha = 0.96f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                BottomActionButton(stringResource(R.string.admin1_reject), AdminColors.Danger, Modifier.weight(1f), onReject)
                // Con comprobante se aprueba desde "Revisar pago"; las de prueba (sin pago), directo como antes.
                if (request.receiptPath != null) {
                    BottomActionButton(stringResource(R.string.admin2_pay_review), KarsyNavy, Modifier.weight(1f), onReviewPayment)
                } else {
                    BottomActionButton(stringResource(R.string.admin1_approve_and_feature), KarsyNavy, Modifier.weight(1f), onApprove)
                }
            }
        }
    }
}

@Composable
private fun SpecText(label: String, value: String, modifier: Modifier = Modifier) {
    val labelWithColon = stringResource(R.string.admin1_label_colon, label)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(labelWithColon) }
            append(value)
        },
        fontFamily = DmSans,
        fontSize = KarsyTextSize.Body,
        color = KarsyCharcoal,
        modifier = modifier
    )
}

@Composable
private fun BottomActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    textColor: Color = KarsyWhite,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(RoundedCornerShape(KarsyRadius.Md))
            .background(color)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            fontFamily = DmSans,
            fontSize = KarsyTextSize.BodyLarge,
            fontWeight = FontWeight.ExtraBold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ApprovedDialog(request: FeaturedRequest, onDone: () -> Unit) {
    val approvedPrefix = stringResource(R.string.admin1_approved_msg_prefix)
    val approvedVehicle = stringResource(R.string.admin1_vehicle_with_year, request.vehicle, request.year)
    val approvedSuffix = stringResource(R.string.admin1_approved_msg_suffix)
    Dialog(onDismissRequest = onDone, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(KarsyRadius.Xl))
                .background(KarsySurface)
                .padding(28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(68.dp).clip(CircleShape).background(ApprovedBg)
            ) {
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = KarsySuccess, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.admin1_approved_title),
                fontFamily = Outfit,
                fontSize = KarsyTextSize.HeadlineLarge,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                buildAnnotatedString {
                    append(approvedPrefix)
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(approvedVehicle)
                    }
                    append(approvedSuffix)
                },
                fontFamily = DmSans,
                fontSize = KarsyTextSize.BodyLarge,
                lineHeight = 21.sp,
                color = KarsyCharcoal,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.admin1_approved_notified),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                color = KarsyMid,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            BottomActionButton(stringResource(R.string.admin1_back_to_requests), KarsyNavy, Modifier.fillMaxWidth(), onDone)
        }
    }
}

@Composable
private fun RejectDialog(onCancel: () -> Unit, onConfirm: (reason: String, comment: String) -> Unit) {
    var reason by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .clip(RoundedCornerShape(KarsyRadius.Xl))
                .background(KarsySurface)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(RejectedBg)
            ) {
                Icon(Icons.Rounded.PriorityHigh, contentDescription = null, tint = AdminColors.Danger, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                stringResource(R.string.admin1_reject_dialog_title),
                fontFamily = Outfit,
                fontSize = KarsyTextSize.HeadlineLarge,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.admin1_reject_dialog_desc),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                lineHeight = 19.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(18.dp))

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                AdminOptions.rejectOptions.forEach { (title, desc) ->
                    val active = reason == title
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KarsyRadius.Md))
                            .background(if (active) karsyTint(KarsyRed, KarsyRedSoft) else KarsyBg)
                            .border(
                                if (active) 1.5.dp else 1.dp,
                                if (active) AdminColors.Danger else KarsyBorder,
                                RoundedCornerShape(KarsyRadius.Md)
                            )
                            .clickable { reason = title }
                            .padding(horizontal = 13.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (active) "●" else "○",
                                fontSize = KarsyTextSize.Body,
                                color = if (active) AdminColors.Danger else KarsyMid,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                title,
                                fontFamily = DmSans,
                                fontSize = KarsyTextSize.Body,
                                fontWeight = FontWeight.ExtraBold,
                                color = KarsyInk
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            desc,
                            fontFamily = DmSans,
                            fontSize = KarsyTextSize.Small,
                            lineHeight = 16.sp,
                            color = KarsyMid,
                            modifier = Modifier.padding(start = 20.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            BasicTextField(
                value = comment,
                onValueChange = { comment = it },
                textStyle = TextStyle(fontFamily = DmSans, fontSize = KarsyTextSize.Body, color = KarsyCharcoal),
                cursorBrush = SolidColor(KarsyInk),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(KarsyRadius.Md))
                            .background(KarsyBg)
                            .border(1.dp, KarsyBorder, RoundedCornerShape(KarsyRadius.Md))
                            .padding(13.dp)
                    ) {
                        if (comment.isEmpty()) {
                            Text(
                                stringResource(R.string.admin1_reject_comment_hint),
                                fontFamily = DmSans,
                                fontSize = KarsyTextSize.Body,
                                color = KarsyMid
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BottomActionButton(
                    stringResource(R.string.admin1_cancel), KarsyBg, Modifier.weight(1f), onCancel, textColor = KarsyMid
                )
                BottomActionButton(
                    stringResource(R.string.admin1_reject_and_notify), AdminColors.Danger, Modifier.weight(1f),
                    onClick = { onConfirm(reason, comment) },
                    enabled = reason.isNotEmpty()
                )
            }
        }
    }
}

/**
 * "Datos para recibir pagos": precio, días, banco y CLABE (config_pagos). Es lo que ven los
 * lotes al pedir un destacado; sin banco o CLABE nadie puede pedirlo.
 */
@Composable
private fun PaymentSettingsCard(vm: AdminViewModel) {
    val datos = vm.paymentSettings
    AdminCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Activo / Incompleto según lo GUARDADO, no lo que se está escribiendo.
            val completo = datos != null && PagoDestacado.disponible(datos.banco, datos.clabe)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.admin2_pay_title),
                        fontFamily = Outfit, fontSize = KarsyTextSize.Title, fontWeight = FontWeight.Bold, color = KarsyInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (datos != null) {
                        AdminBadge(
                            stringResource(if (completo) R.string.admin2_pay_active else R.string.admin2_pay_incomplete),
                            if (completo) BadgeTone.Success else BadgeTone.Warning
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.admin2_pay_desc), fontFamily = DmSans, fontSize = KarsyTextSize.Small, lineHeight = 17.sp, color = AdminColors.NeutralText)
            }
            if (datos == null) {
                if (vm.loading) SkeletonListRow(circle = false)
                else Text(stringResource(R.string.admin2_pay_load_error), fontFamily = DmSans, fontSize = KarsyTextSize.Body, color = BadgeTone.Danger.fg)
                return@Column
            }
            // Lo guardado, con el mismo formato que los campos (para detectar cambios y Descartar).
            val precio0 = PagoDestacado.monto(datos.precio).removePrefix("$").replace(",", "")
            val dias0 = datos.dias.toString()
            val banco0 = datos.banco.orEmpty()
            // El campo Banco solo se usa con un banco que no se reconoce por la CLABE: si el guardado
            // salió de la CLABE, empieza vacío (no se arrastra a una CLABE de otro banco).
            val bancoManual0 = if (PagoDestacado.bancoDeClabe(datos.clabe.orEmpty()) == null) banco0 else ""
            val clabe0 = datos.clabe.orEmpty()
            // Se vuelven a llenar cuando cambian los datos guardados (p. ej. después de Guardar).
            var precio by rememberSaveable(datos) { mutableStateOf(precio0) }
            var dias by rememberSaveable(datos) { mutableStateOf(dias0) }
            var banco by rememberSaveable(datos) { mutableStateOf(bancoManual0) }
            var clabe by rememberSaveable(datos) { mutableStateOf(clabe0) }

            if (!completo) {
                Text(
                    stringResource(R.string.admin2_pay_missing),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Body,
                    lineHeight = 19.sp,
                    color = BadgeTone.Warning.fg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KarsyRadius.Md))
                        .background(BadgeTone.Warning.bg)
                        .padding(12.dp)
                )
            }

            // Precio del destacado
            PaymentSubtitle(stringResource(R.string.admin2_pay_price_section))
            val diasSufijo = stringResource(R.string.admin2_pay_days_suffix)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PaymentField(
                    stringResource(R.string.admin2_pay_price_label), precio, KeyboardType.Decimal, !vm.working, Modifier.weight(1f),
                    prefix = "$", suffix = "MXN"
                ) { precio = it.filter { c -> c.isDigit() || c == '.' }.take(9) }
                PaymentField(
                    stringResource(R.string.admin2_pay_duration), dias, KeyboardType.Number, !vm.working, Modifier.weight(1f),
                    suffix = diasSufijo
                ) { dias = it.filter(Char::isDigit).take(3) }
            }
            val precioOk = PagoDestacado.leerPrecio(precio)?.takeIf { it > 0 && it <= PagoDestacado.MAX_PRECIO }
            val diasOk = dias.trim().toIntOrNull()?.takeIf { it in 1..PagoDestacado.MAX_DIAS }
            if (precioOk != null && diasOk != null) {
                Text(
                    stringResource(R.string.admin2_pay_summary, PagoDestacado.monto(precioOk), diasOk),
                    fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyTextSecondary
                )
            }

            // Cuenta para recibir transferencias: primero la CLABE, de ella sale el banco.
            PaymentSubtitle(stringResource(R.string.admin2_pay_account_section))
            val clabeCompleta = clabe.length == PagoDestacado.LARGO_CLABE
            val clabeOk = PagoDestacado.clabeValida(clabe)
            // El banco sale de la CLABE; solo se escribe si su banco no está en la lista.
            val bancoDetectado = if (clabeOk) PagoDestacado.bancoDeClabe(clabe) else null
            val bancoFinal = bancoDetectado ?: banco.trim()
            Column {
                PaymentField(
                    stringResource(R.string.admin2_pay_clabe), clabe, KeyboardType.Number, !vm.working,
                    monospace = true, isError = clabeCompleta && !clabeOk
                ) {
                    clabe = PagoDestacado.limpiarClabe(it)
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) {
                        if (clabeOk) {
                            val detectado = bancoDetectado
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Verified, contentDescription = null, tint = BadgeTone.Success.fg, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    stringResource(R.string.admin2_pay_clabe_ok) + (detectado?.let { " · $it" } ?: ""),
                                    fontFamily = DmSans, fontSize = KarsyTextSize.Small, fontWeight = FontWeight.SemiBold, color = BadgeTone.Success.fg
                                )
                            }
                        } else if (clabeCompleta) {
                            Text(
                                stringResource(R.string.admin2_pay_error_clabe_digit),
                                fontFamily = DmSans, fontSize = KarsyTextSize.Small, lineHeight = 16.sp, color = BadgeTone.Danger.fg
                            )
                        }
                    }
                    Text(
                        "${clabe.length}/${PagoDestacado.LARGO_CLABE}",
                        fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyTextSecondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (clabeOk && bancoDetectado == null) Column {
                PaymentField(stringResource(R.string.admin2_pay_bank), banco, KeyboardType.Text, !vm.working) {
                    banco = it.uppercase().take(PagoDestacado.MAX_BANCO)
                }
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.admin2_pay_bank_help), fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyTextSecondary)
            }

            // Vista previa: lo que verá el lote en la ventana "Destacar".
            if (bancoFinal.isNotEmpty() && bancoFinal.length <= PagoDestacado.MAX_BANCO && clabeOk) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(KarsyRadius.Md))
                        .background(karsyTint(KarsyTeal, KarsyTealSoft))
                        .padding(12.dp)
                ) {
                    Text(
                        stringResource(R.string.admin2_pay_preview),
                        fontFamily = DmSans, fontSize = KarsyTextSize.Small, fontWeight = FontWeight.Bold, color = KarsyTealText
                    )
                    PaymentPreviewLine(stringResource(R.string.admin2_pay_bank), bancoFinal)
                    PaymentPreviewLine("CLABE", PagoDestacado.clabeAgrupada(clabe), monospace = true)
                    if (precioOk != null && diasOk != null) {
                        Text(
                            "${PagoDestacado.monto(precioOk)} MXN · $diasOk $diasSufijo",
                            fontFamily = DmSans, fontSize = KarsyTextSize.Body, fontWeight = FontWeight.Bold, color = KarsyInk
                        )
                    }
                }
            }

            // Pie: Guardar solo con cambios válidos; Descartar vuelve a lo guardado.
            // Se compara el valor, no el texto: "300" y "300.00" o "BBVA " y "BBVA" no son cambios.
            val cambios = PagoDestacado.leerPrecio(precio) != datos.precio ||
                dias.trim().toIntOrNull() != datos.dias ||
                bancoFinal != banco0 ||
                clabe != clabe0
            val valido = PagoDestacado.errorConfig(precio, dias, bancoFinal, clabe) == null &&
                // Con CLABE, el banco no puede quedar vacío (los lotes no sabrían a cuál transferir).
                (clabe.isEmpty() || bancoFinal.isNotEmpty())
            if (cambios) {
                Text(
                    stringResource(R.string.admin2_pay_unsaved),
                    fontFamily = DmSans, fontSize = KarsyTextSize.Small, fontWeight = FontWeight.SemiBold, color = BadgeTone.Warning.fg
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (cambios) {
                    BottomActionButton(
                        stringResource(R.string.admin2_pay_discard), KarsyBg, Modifier.weight(1f),
                        onClick = {
                            precio = precio0
                            dias = dias0
                            banco = bancoManual0
                            clabe = clabe0
                        },
                        enabled = !vm.working,
                        textColor = KarsyMid
                    )
                }
                BottomActionButton(
                    stringResource(R.string.admin2_pay_save_changes),
                    KarsyNavy,
                    Modifier.weight(1f),
                    onClick = { vm.savePaymentSettings(precio, dias, bancoFinal, clabe) },
                    enabled = cambios && valido && !vm.working
                )
            }
        }
    }
}

@Composable
private fun PaymentSubtitle(text: String) {
    Text(text, fontFamily = DmSans, fontSize = KarsyTextSize.Body, fontWeight = FontWeight.Bold, color = KarsyInk)
}

@Composable
private fun PaymentPreviewLine(label: String, value: String, monospace: Boolean = false) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = KarsyTextSecondary)) { append("$label: ") }
            withStyle(SpanStyle(fontFamily = if (monospace) FontFamily.Monospace else DmSans, fontWeight = FontWeight.SemiBold)) {
                append(value)
            }
        },
        fontFamily = DmSans, fontSize = KarsyTextSize.Body, color = KarsyInk
    )
}

@Composable
private fun PaymentField(
    label: String,
    value: String,
    keyboardType: KeyboardType,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    monospace: Boolean = false,
    prefix: String? = null,
    suffix: String? = null,
    isError: Boolean = false,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = DmSans, fontSize = KarsyTextSize.Small) },
        prefix = prefix?.let { { Text(it, fontFamily = DmSans, fontSize = KarsyTextSize.BodyLarge, color = KarsyTextSecondary) } },
        suffix = suffix?.let { { Text(it, fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyTextSecondary) } },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = TextStyle(
            fontFamily = if (monospace) FontFamily.Monospace else DmSans,
            fontSize = KarsyTextSize.BodyLarge,
            color = KarsyCharcoal
        ),
        shape = RoundedCornerShape(KarsyRadius.Md),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KarsyNavy,
            unfocusedBorderColor = KarsyBorder,
        ),
        modifier = modifier.fillMaxWidth()
    )
}
