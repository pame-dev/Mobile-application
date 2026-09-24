package com.pame.karsy.feature.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.model.Car
import com.pame.karsy.data.model.User

/** Pantalla "Mi perfil" (WebProfileView del mockup). */
@Composable
fun ProfileScreen(
    userMode: UserMode,
    onBack: () -> Unit,
    onPanel: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    onCarClick: (Long) -> Unit,
    vm: ProfileViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.load() }
    val context = LocalContext.current
    val user = vm.user
    var editOpen by rememberSaveable { mutableStateOf(false) }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.uploadAvatar(context, uri)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = "Mi perfil",
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        ) {
            IconButton(onClick = onHistory) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = "Historial",
                    tint = KarsyNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Configuración",
                    tint = KarsyNavy,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
        ) {
            if (user != null) {
                ProfileHeaderCard(
                    user = user,
                    userMode = userMode,
                    onEdit = { editOpen = true }
                )
                Spacer(Modifier.height(16.dp))
            } else if (vm.loading) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KarsyTeal)
                }
            }
            vm.message?.let {
                Text(
                    it,
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    color = KarsyTeal,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                vm.stats.forEach { (num, label) ->
                    StatBox(num = num, label = label, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))

            PostsCard(
                cars = vm.cars,
                panelLabel = if (userMode.isAdmin) "Panel admin" else "+ Mi panel",
                onPanel = onPanel,
                onCarClick = onCarClick
            )
        }
    }

    if (editOpen && user != null) {
        EditProfileDialog(
            user = user,
            saving = vm.saving,
            onDismiss = { editOpen = false },
            onSave = { updated -> vm.save(updated) { editOpen = false } },
            onPickPhoto = {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

@Composable
private fun ProfileHeaderCard(user: User, userMode: UserMode, onEdit: () -> Unit) {
    ProfileCard {
      Box(modifier = Modifier.fillMaxWidth()) {
        // Portada con degradado
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(ProfileCoverBrush)
        )
        // El avatar se superpone 44dp sobre la portada (marginTop: -44 del mockup).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 56.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    ProfileAvatar(
                        name = user.displayName,
                        avatarUrl = user.avatarUrl,
                        size = 88.dp,
                        initialsSize = 30.sp,
                        modifier = Modifier
                            .shadow(6.dp, CircleShape, ambientColor = CardShadow, spotColor = CardShadow)
                            .border(4.dp, KarsyWhite, CircleShape)
                    )
                    // Indicador "en línea"
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-3).dp, y = (-3).dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(KarsyWhite)
                            .padding(3.dp)
                            .clip(CircleShape)
                            .background(KarsyTeal)
                    )
                }
                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, KarsyBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = KarsyWhite,
                        contentColor = KarsyNavy
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 9.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        "Editar perfil",
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = user.displayName,
                        fontFamily = Outfit,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = KarsyNavy,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (userMode == UserMode.LOTE || userMode.isAdmin) {
                        AccountBadge(if (userMode.isAdmin) "Administrador" else "Lote")
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "@" + user.email.substringBefore("@"),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = listOf(user.bio, user.location).filter { it.isNotBlank() }.joinToString(" · "),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    color = KarsyTextSecondary
                )
            }
        }
      }
    }
}

@Composable
private fun AccountBadge(text: String) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = KarsyTeal,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(KarsyTealLight)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun StatBox(num: String, label: String, modifier: Modifier = Modifier) {
    ProfileCard(modifier = modifier, radius = 14.dp, elevation = 1.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                num,
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = KarsyNavy
            )
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                fontFamily = DmSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = KarsyMid
            )
        }
    }
}

@Composable
private fun PostsCard(cars: List<Car>, panelLabel: String, onPanel: () -> Unit, onCarClick: (Long) -> Unit) {
    ProfileCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Mis publicaciones",
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = KarsyNavy
            )
            Button(
                onClick = onPanel,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KarsyNavy,
                    contentColor = KarsyWhite
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 7.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    panelLabel,
                    fontFamily = DmSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
        RowDivider()
        Column(
            modifier = Modifier.padding(3.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            if (cars.isEmpty()) {
                Text(
                    "Aún no tienes publicaciones.",
                    fontFamily = DmSans,
                    fontSize = 13.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(18.dp)
                )
            }
            cars.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { car ->
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(ImagePlaceholder)
                                .clickable { onCarClick(car.id) }
                        ) {
                            AsyncImage(
                                model = car.imageUrl,
                                contentDescription = "${car.title} ${car.year}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Estado de moderación / venta cuando no está activo.
                            if (car.status != "Activo") Text(
                                car.status,
                                fontFamily = DmSans,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KarsyWhite,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(5.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(KarsyNavy.copy(alpha = 0.75f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}
