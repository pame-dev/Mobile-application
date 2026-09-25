package com.pame.karsy.feature.register

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.TermsCheckbox
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight

/** Formulario de registro de una cuenta de Lote (agencia). Crea el usuario en Supabase Auth. */
@Composable
fun RegisterLoteScreen(
    onBack: () -> Unit,
    onCreated: (SessionAccount) -> Unit,
    onVerifyEmail: (String) -> Unit,
    vm: RegisterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.logo = uri
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
    ) {
        BackTopBar(onBack = onBack)
        RegisterHeader(
            title = "Crear perfil de lote",
            subtitle = "Registra tu lote para comenzar a publicar vehículos."
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        ) {
            SectionLabel("Datos del responsable")
            FormInput(
                label = "Nombre del responsable", placeholder = "Ingresa el nombre",
                value = vm.nombre, onValueChange = { vm.nombre = it }, error = vm.errorFor("nombre")
            )
            FormInput(
                label = "Apellido del responsable", placeholder = "Ingresa el apellido",
                value = vm.apellido, onValueChange = { vm.apellido = it }, error = vm.errorFor("apellido")
            )
            FormInput(
                label = "Correo electrónico", placeholder = "correo@ejemplo.com", keyboardType = KeyboardType.Email,
                value = vm.correo, onValueChange = { vm.correo = it }, error = vm.errorFor("correo")
            )

            SectionLabel("Contacto")
            ContactFields(vm, phoneLabel = "Teléfono comercial")

            SectionLabel("Información del lote")
            FormInput(
                label = "Nombre del lote", placeholder = "Ej. Auto Premium",
                value = vm.nombreLote, onValueChange = { vm.nombreLote = it }, error = vm.errorFor("nombreLote")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = "Calle", placeholder = "Ej. Av. Juárez", modifier = Modifier.weight(2f),
                    value = vm.calle, onValueChange = { vm.calle = it }, error = vm.errorFor("calle")
                )
                FormInput(
                    label = "Número", placeholder = "123", modifier = Modifier.weight(1f),
                    value = vm.numero, onValueChange = { vm.numero = it }, error = vm.errorFor("numero")
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = "Colonia", placeholder = "Ej. Centro", modifier = Modifier.weight(2f),
                    value = vm.colonia, onValueChange = { vm.colonia = it }, error = vm.errorFor("colonia")
                )
                FormInput(
                    label = "C.P.", placeholder = "27000", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f),
                    value = vm.codigoPostal, onValueChange = { vm.codigoPostal = it }, error = vm.errorFor("codigoPostal")
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = "Ciudad", placeholder = "Ej. Torreón", modifier = Modifier.weight(1f),
                    value = vm.ciudad, onValueChange = { vm.ciudad = it }, error = vm.errorFor("ciudad")
                )
                FormInput(
                    label = "Estado", placeholder = "Ej. Coahuila", modifier = Modifier.weight(1f),
                    value = vm.estado, onValueChange = { vm.estado = it }, error = vm.errorFor("estado")
                )
            }
            FormInput(
                label = "Descripción del lote",
                placeholder = "Describe brevemente tu lote, los servicios que ofrece y el tipo de vehículos que maneja.",
                singleLine = false,
                minLines = 4,
                value = vm.descripcion,
                onValueChange = { vm.descripcion = it }
            )

            SectionLabel("Foto de perfil")
            PhotoPlaceholder(
                imageUri = vm.logo,
                onClick = {
                    pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "Logo del lote o imagen de perfil",
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

            SectionLabel("Seguridad")
            FormInput(
                label = "Contraseña", placeholder = "Ingresa una contraseña", isPassword = true,
                value = vm.password, onValueChange = { vm.password = it }, error = vm.errorFor("password")
            )
            FormInput(
                label = "Confirmar contraseña", placeholder = "Repite tu contraseña", isPassword = true,
                value = vm.confirmPassword, onValueChange = { vm.confirmPassword = it }, error = vm.passwordMismatch
            )

            Spacer(Modifier.height(24.dp))
            TermsCheckbox(checked = vm.acceptedTerms, onCheckedChange = { vm.acceptedTerms = it })
            PrimaryButton(
                text = if (vm.loading) "Creando perfil…" else "Crear perfil",
                enabled = !vm.loading,
                onClick = { vm.submit(esLote = true, context = context, onCreated = onCreated, onVerifyEmail = onVerifyEmail) },
                modifier = Modifier.alpha(if (vm.acceptedTerms) 1f else 0.5f)
            )
            RegisterError(vm.error)
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Círculo punteado "Agregar foto"; muestra la imagen elegida. */
@Composable
private fun PhotoPlaceholder(imageUri: Uri?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Logo del lote",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(100.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        )
        return
    }
    Box(
        modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(KarsyTealLight)
            .drawBehind {
                val stroke = 2.dp.toPx()
                drawCircle(
                    color = KarsyTeal,
                    radius = size.minDimension / 2 - stroke / 2,
                    style = Stroke(
                        width = stroke,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))
                    )
                )
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(22.dp))
            Text(
                "Agregar\nfoto",
                fontFamily = DmSans,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                textAlign = TextAlign.Center
            )
        }
    }
}
