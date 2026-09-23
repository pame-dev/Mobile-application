package com.pame.karsy.feature.register

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

private enum class ClientType { PARTICULAR, LOTE }

/** Elección del tipo de cuenta: Particular o Lote. Se navega al pulsar "Continuar". */
@Composable
fun RegisterTypeScreen(onBack: () -> Unit, onParticular: () -> Unit, onLote: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf<ClientType?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        BackTopBar(onBack = onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            RegisterHeader(title = "Registro", subtitle = "Selecciona qué tipo de cliente eres")
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp)) {
                TypeCard(
                    icon = Icons.Outlined.PersonOutline,
                    title = "Particular",
                    desc = "Compra y vende vehículos como persona particular.",
                    active = selected == ClientType.PARTICULAR,
                    onClick = { selected = ClientType.PARTICULAR }
                )
                TypeCard(
                    icon = Icons.Outlined.BusinessCenter,
                    title = "Lote",
                    desc = "Publica y administra vehículos de tu lote o agencia.",
                    active = selected == ClientType.LOTE,
                    onClick = { selected = ClientType.LOTE }
                )
            }
        }
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp)
        ) {
            PrimaryButton(
                text = "Continuar",
                onClick = {
                    when (selected) {
                        ClientType.PARTICULAR -> onParticular()
                        ClientType.LOTE -> onLote()
                        null -> Unit
                    }
                },
                modifier = Modifier.alpha(if (selected != null) 1f else 0.5f)
            )
            Text(
                "Podrás completar tu perfil después.",
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun TypeCard(
    icon: ImageVector,
    title: String,
    desc: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val bg by animateColorAsState(if (active) KarsyTealLight else KarsyWhite, label = "cardBg")
    val iconBg by animateColorAsState(if (active) KarsyTeal else KarsyTealLight, label = "iconBg")
    Row(
        Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .shadow(
                if (active) 8.dp else 3.dp, shape,
                ambientColor = if (active) KarsyTeal else KarsyNavy,
                spotColor = if (active) KarsyTeal else KarsyNavy
            )
            .clip(shape)
            .background(bg)
            .border(if (active) 2.dp else 1.5.dp, if (active) KarsyTeal else KarsyBorder, shape)
            .clickable(onClick = onClick)
            .padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (active) KarsyWhite else KarsyTeal, modifier = Modifier.size(34.dp))
        }
        Column {
            Text(title, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = KarsyNavy)
            Spacer(Modifier.height(4.dp))
            Text(desc, fontFamily = DmSans, fontSize = 14.sp, lineHeight = 19.6.sp, color = KarsyMid)
        }
    }
}
