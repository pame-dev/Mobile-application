package com.pame.karsy.feature.cardetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.pame.karsy.data.model.SellerContact
import com.pame.karsy.feature.profile.ProfileAvatar

internal val SheetScrim = Color(0x800D2B45)
internal val SheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

/** Hoja inferior "Información de contacto" con los botones de llamar y enviar correo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactSheet(
    detail: CarDetail,
    contact: SellerContact?,
    loading: Boolean,
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    val context = LocalContext.current
    val telefono = contact?.telefono
    val correo = contact?.correo

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = KarsyWhite,
        scrimColor = SheetScrim,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 28.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFD9E1E6))
            )

            Box(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                Text(
                    "Información de contacto",
                    textAlign = TextAlign.Center,
                    fontFamily = Outfit,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).align(Alignment.CenterEnd)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = KarsyMid, modifier = Modifier.size(18.dp))
                }
            }

            val cardShape = RoundedCornerShape(16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .shadow(4.dp, cardShape, ambientColor = KarsyNavy, spotColor = KarsyNavy.copy(alpha = 0.3f))
                    .clip(cardShape)
                    .background(KarsyWhite)
                    .border(1.dp, KarsyBorder, cardShape)
                    .padding(16.dp)
            ) {
                ProfileAvatar(
                    name = detail.contactoNombre,
                    avatarUrl = detail.sellerAvatar,
                    size = 56.dp,
                    initialsSize = 19.sp,
                    modifier = Modifier
                        .border(2.dp, KarsyBorder, CircleShape)
                        .clickable(onClick = onOpenProfile)
                )
                Column {
                    Text(
                        detail.contactoNombre,
                        fontFamily = Outfit,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KarsyNavy,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (loading) {
                        CircularProgressIndicator(color = KarsyTeal, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            telefono ?: "Sin teléfono registrado",
                            fontFamily = DmSans,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (telefono != null) KarsyTeal else KarsyMid,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                        Text(correo ?: "—", fontFamily = DmSans, fontSize = 12.sp, color = KarsyCharcoal)
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(KarsyBg)
                    .padding(12.dp)
            ) {
                Button(
                    onClick = {
                        telefono?.let {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + it.filter { c -> c.isDigit() || c == '+' })))
                        }
                    },
                    enabled = telefono != null,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text(
                        "Llamar al vendedor",
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                OutlinedButton(
                    onClick = {
                        correo?.let {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$it"))
                                .putExtra(Intent.EXTRA_SUBJECT, "Interés en ${detail.car.title} ${detail.car.year} (Karsy)")
                            runCatching { context.startActivity(intent) }
                        }
                    },
                    enabled = correo != null,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.5.dp, KarsyTeal),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = KarsyWhite, contentColor = KarsyNavy),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.MailOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text(
                        "Enviar correo",
                        fontFamily = Outfit,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
