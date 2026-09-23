package com.pame.karsy.feature.register

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.TermsCheckbox
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight

/** Formulario de registro de una cuenta de Lote (agencia). */
@Composable
fun RegisterLoteScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

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
            // TODO: guardar los valores en un ViewModel/estado del formulario y validarlos
            SectionLabel("Datos del responsable")
            FormInput(label = "Nombre del responsable", placeholder = "Ingresa el nombre")
            FormInput(label = "Apellido del responsable", placeholder = "Ingresa el apellido")
            FormInput(label = "Correo electrónico", placeholder = "correo@ejemplo.com", keyboardType = KeyboardType.Email)
            FormInput(label = "Teléfono comercial", placeholder = "10 dígitos", keyboardType = KeyboardType.Phone)

            SectionLabel("Información del lote")
            FormInput(label = "Nombre del lote", placeholder = "Ej. Auto Premium")
            FormInput(label = "Dirección del lote", placeholder = "Ingresa la dirección")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // TODO: convertir Ciudad/Estado en selectores con el catálogo de la base de datos
                FormInput(label = "Ciudad", placeholder = "Selecciona una ciudad", modifier = Modifier.weight(1f))
                FormInput(label = "Estado", placeholder = "Selecciona un estado", modifier = Modifier.weight(1f))
            }
            FormInput(
                label = "Descripción del lote",
                placeholder = "Describe brevemente tu lote, los servicios que ofrece y el tipo de vehículos que maneja.",
                singleLine = false,
                minLines = 4
            )

            SectionLabel("Foto de perfil")
            PhotoPlaceholder(
                onClick = {
                    // TODO: abrir el selector de imágenes (PickVisualMedia) y subir el logo a Supabase Storage
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
            FormInput(label = "Contraseña", placeholder = "Ingresa una contraseña", isPassword = true)
            FormInput(label = "Confirmar contraseña", placeholder = "Repite tu contraseña", isPassword = true)

            Spacer(Modifier.height(24.dp))
            TermsCheckbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
            PrimaryButton(
                text = "Crear perfil",
                onClick = {
                    // TODO: supabase.auth.signUp(email, password) e insertar en public.cuentas
                    //  (tipo_cuenta = 'lote'), public.perfiles_lote y public.telefonos_contacto
                    if (acceptedTerms) onCreated()
                },
                modifier = Modifier.alpha(if (acceptedTerms) 1f else 0.5f)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Círculo punteado "Agregar foto" (sin selector real todavía). */
@Composable
private fun PhotoPlaceholder(onClick: () -> Unit, modifier: Modifier = Modifier) {
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
