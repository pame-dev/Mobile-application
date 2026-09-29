package com.pame.karsy.feature.admin

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
import com.pame.karsy.R
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.theme.DmSans
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

private val FeaturedYellowBg = Color(0xFFFFF4CC)
private val FeaturedYellowFg = Color(0xFF8A6415)
private val ApprovedBg = Color(0xFFEDF7ED)
private val RejectedBg = Color(0xFFFDECEC)
private val RejectedFg = Color(0xFFB42318)

@Composable
fun AdminFeaturedSection(vm: AdminViewModel) {
    val requests = vm.featuredRequests
    var filter by rememberSaveable { mutableStateOf(FeaturedStatus.Pendiente) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showApproved by remember { mutableStateOf(false) }
    var showReject by remember { mutableStateOf(false) }

    val selected = requests.firstOrNull { it.id == selectedId }

    if (selected != null) {
        BackHandler { selectedId = null }
        FeaturedRequestDetail(
            request = selected,
            onBackToList = { selectedId = null },
            onReject = { showReject = true },
            onApprove = {
                // admin_resolver_destacado: destaca 1 mes desde hoy y registra la acción.
                vm.resolveFeatured(selected, approve = true)
                showApproved = true
            }
        )

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

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)
        ) {
            FeaturedStatus.entries.forEach { status ->
                val active = filter == status
                val count = requests.count { it.status == status }
                val label = when (status) {
                    FeaturedStatus.Pendiente -> stringResource(R.string.admin1_filter_pending)
                    FeaturedStatus.Aprobada -> stringResource(R.string.admin1_filter_approved)
                    FeaturedStatus.Rechazada -> stringResource(R.string.admin1_filter_rejected)
                }
                Text(
                    stringResource(R.string.admin1_filter_with_count, label, count),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) KarsyWhite else KarsyCharcoal,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) KarsyNavy else KarsyWhite)
                        .border(
                            if (active) 1.5.dp else 1.dp,
                            if (active) KarsyNavy else KarsyBorder,
                            RoundedCornerShape(999.dp)
                        )
                        .clickable { filter = status }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                )
            }
        }

        if (visible.isEmpty()) {
            AdminCard {
                Text(
                    stringResource(R.string.admin1_featured_empty),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    color = KarsyMid,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                visible.forEach { request ->
                    FeaturedRequestCard(request, onClick = { selectedId = request.id })
                }
            }
        }
    }
}

@Composable
private fun FeaturedRequestCard(request: FeaturedRequest, onClick: () -> Unit) {
    val requestPrefix = stringResource(R.string.admin1_user_requested_prefix)
    val requestSuffix = stringResource(R.string.admin1_user_requested_suffix)
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.1f), spotColor = KarsyNavy.copy(alpha = 0.1f))
            .clip(shape)
            .background(KarsyWhite)
            .border(1.dp, KarsyBorder, shape)
            .clickable(onClick = onClick)
    ) {
        Box {
            AsyncImage(
                model = request.image,
                contentDescription = request.vehicle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(170.dp).background(KarsyBg)
            )
            val (text, bg, fg) = when (request.status) {
                FeaturedStatus.Aprobada -> Triple(stringResource(R.string.admin1_badge_featured), ApprovedBg, KarsySuccess)
                FeaturedStatus.Rechazada -> Triple(stringResource(R.string.admin1_badge_rejected), RejectedBg, RejectedFg)
                FeaturedStatus.Pendiente -> Triple(stringResource(R.string.admin1_badge_requested), FeaturedYellowBg, FeaturedYellowFg)
            }
            Text(
                text,
                fontFamily = DmSans,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(bg)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp)) {
            Text(
                request.vehicle,
                fontFamily = Outfit,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
            ) {
                Text(
                    request.year.toString(),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = KarsyMid,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        request.price.substringBeforeLast(" "),
                        fontFamily = Outfit,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyTeal
                    )
                    Text(request.price.substringAfterLast(" "), fontFamily = DmSans, fontSize = 10.sp, color = KarsyMid)
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .background(KarsyBg)
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = KarsyCharcoal,
                    modifier = Modifier.size(15.dp).padding(top = 2.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    buildAnnotatedString {
                        append(requestPrefix)
                        withStyle(SpanStyle(color = KarsyNavy, fontWeight = FontWeight.Bold)) {
                            append(request.user)
                        }
                        append(requestSuffix)
                    },
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = KarsyCharcoal
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(request.time, fontFamily = DmSans, fontSize = 11.sp, color = KarsyMid)
        }
    }
}

@Composable
private fun FeaturedRequestDetail(
    request: FeaturedRequest,
    onBackToList: () -> Unit,
    onReject: () -> Unit,
    onApprove: () -> Unit,
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
            Text(
                stringResource(R.string.admin1_back_to_requests_arrow),
                fontFamily = DmSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyTeal,
                modifier = Modifier
                    .clickable(onClick = onBackToList)
                    .padding(bottom = 14.dp)
            )
            AdminSectionHeader(
                title = stringResource(R.string.admin1_review_title),
                description = stringResource(R.string.admin1_review_desc)
            )

            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape, ambientColor = KarsyNavy.copy(alpha = 0.1f), spotColor = KarsyNavy.copy(alpha = 0.1f))
                    .clip(shape)
                    .background(KarsyWhite)
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
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = FeaturedYellowFg,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(FeaturedYellowBg)
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(request.year.toString(), fontFamily = DmSans, fontSize = 13.sp, color = KarsyMid)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                request.vehicle,
                                fontFamily = Outfit,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            request.price,
                            fontFamily = Outfit,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KarsyTeal
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    SectionLabel(stringResource(R.string.admin1_specs))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
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
                        fontSize = 14.sp,
                        lineHeight = 23.sp,
                        color = KarsyTextSecondary
                    )

                    SectionLabel(stringResource(R.string.admin1_requester))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KarsyWhite)
                            .border(1.dp, KarsyBorder, RoundedCornerShape(14.dp))
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy
                            )
                            Text(
                                stringResource(R.string.admin1_view_profile),
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyTeal,
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
                    .background(KarsyWhite.copy(alpha = 0.96f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                BottomActionButton(stringResource(R.string.admin1_reject), AdminColors.Danger, Modifier.weight(1f), onReject)
                BottomActionButton(stringResource(R.string.admin1_approve_and_feature), KarsyNavy, Modifier.weight(1f), onApprove)
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
        fontSize = 13.sp,
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
            .clip(RoundedCornerShape(13.dp))
            .background(color)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Text(
            text,
            fontFamily = DmSans,
            fontSize = 14.sp,
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
                .clip(RoundedCornerShape(18.dp))
                .background(KarsyWhite)
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
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
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
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = KarsyCharcoal,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.admin1_approved_notified),
                fontFamily = DmSans,
                fontSize = 12.sp,
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
                .clip(RoundedCornerShape(20.dp))
                .background(KarsyWhite)
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
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyNavy,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.admin1_reject_dialog_desc),
                fontFamily = DmSans,
                fontSize = 13.sp,
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (active) Color(0xFFFFF7F7) else KarsyBg)
                            .border(
                                if (active) 1.5.dp else 1.dp,
                                if (active) AdminColors.Danger else KarsyBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { reason = title }
                            .padding(horizontal = 13.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (active) "●" else "○",
                                fontSize = 13.sp,
                                color = if (active) AdminColors.Danger else KarsyMid,
                                modifier = Modifier.width(20.dp)
                            )
                            Text(
                                title,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = KarsyNavy
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            desc,
                            fontFamily = DmSans,
                            fontSize = 11.5.sp,
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
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = KarsyCharcoal),
                cursorBrush = SolidColor(KarsyNavy),
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KarsyBg)
                            .border(1.dp, KarsyBorder, RoundedCornerShape(12.dp))
                            .padding(13.dp)
                    ) {
                        if (comment.isEmpty()) {
                            Text(
                                stringResource(R.string.admin1_reject_comment_hint),
                                fontFamily = DmSans,
                                fontSize = 13.sp,
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
