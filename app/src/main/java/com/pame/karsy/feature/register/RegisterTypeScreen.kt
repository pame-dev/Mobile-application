package com.pame.karsy.feature.register

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTealText
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

private enum class ClientType { PARTICULAR, LOTE }

/** Elección del tipo de cuenta: Particular o Lote. Se navega al pulsar "Continuar". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegisterTypeScreen(onBack: () -> Unit, onParticular: () -> Unit, onLote: () -> Unit, onLogin: () -> Unit) {
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
            RegisterHeader(title = stringResource(R.string.auth_register_title), subtitle = stringResource(R.string.auth_register_type_subtitle))
            // Dos tarjetas grandes que llenan la pantalla: así no parece que falten opciones.
            Column(
                Modifier.padding(start = 24.dp, end = 24.dp, top = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TypeCard(
                    icon = Icons.Outlined.PersonOutline,
                    title = stringResource(R.string.auth_type_particular),
                    desc = stringResource(R.string.auth_type_particular_desc),
                    active = selected == ClientType.PARTICULAR,
                    onClick = { selected = ClientType.PARTICULAR }
                )
                TypeCard(
                    icon = Icons.Outlined.BusinessCenter,
                    title = stringResource(R.string.auth_type_lote),
                    desc = stringResource(R.string.auth_type_lote_desc),
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
                text = stringResource(R.string.auth_continue),
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
                stringResource(R.string.auth_complete_profile_later),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.BodyLarge,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            )
            // FlowRow: en pantallas angostas el enlace baja completo a la siguiente línea.
            FlowRow(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.Center,
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.auth_have_account_question), fontFamily = DmSans, fontSize = KarsyTextSize.Title, color = KarsyCharcoal)
                Text(
                    stringResource(R.string.auth_sign_in_here),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Title,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTealText,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clip(RoundedCornerShape(KarsyRadius.Sm))
                        .clickable(role = Role.Button, onClick = onLogin)
                        .padding(horizontal = 4.dp, vertical = 10.dp)
                )
            }
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
    val shape = RoundedCornerShape(KarsyRadius.Lg)
    val bg by animateColorAsState(if (active) KarsyTealLight else KarsySurface, label = "cardBg")
    val iconBg by animateColorAsState(if (active) KarsyTeal else KarsyTealLight, label = "iconBg")
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .shadow(
                if (active) 8.dp else 3.dp, shape,
                ambientColor = if (active) KarsyTeal else KarsyNavy,
                spotColor = if (active) KarsyTeal else KarsyNavy
            )
            .clip(shape)
            .background(bg)
            .border(if (active) 2.dp else 1.5.dp, if (active) KarsyTeal else KarsyBorder, shape)
            // Opción seleccionable: TalkBack anuncia si está elegida.
            .selectable(selected = active, role = Role.RadioButton, onClick = onClick)
            .padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(KarsyRadius.Lg))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (active) KarsyWhite else KarsyTeal, modifier = Modifier.size(40.dp))
        }
        Column {
            Text(title, fontFamily = Outfit, fontWeight = FontWeight.Bold, fontSize = KarsyTextSize.HeadlineLarge, color = KarsyInk)
            Spacer(Modifier.height(6.dp))
            Text(desc, fontFamily = DmSans, fontSize = KarsyTextSize.Subtitle, lineHeight = 21.sp, color = KarsyMid)
        }
    }
}
