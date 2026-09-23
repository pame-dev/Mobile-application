package com.pame.karsy.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.TermsCheckbox
import com.pame.karsy.core.theme.KarsyBg

/** Formulario de registro de una cuenta Particular. */
@Composable
fun RegisterParticularScreen(onBack: () -> Unit, onCreated: () -> Unit) {
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
    ) {
        BackTopBar(onBack = onBack)
        RegisterHeader(
            title = "Crear perfil",
            subtitle = "Registra tus datos para comenzar a ver y vender vehículos"
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        ) {
            // TODO: guardar los valores en un ViewModel/estado del formulario y validarlos
            SectionLabel("Información personal")
            FormInput(label = "Nombre", placeholder = "Ingresa tu nombre")
            FormInput(label = "Apellido", placeholder = "Ingresa tu apellido")
            FormInput(label = "Correo electrónico", placeholder = "correo@ejemplo.com", keyboardType = KeyboardType.Email)
            FormInput(label = "Teléfono", placeholder = "10 dígitos", keyboardType = KeyboardType.Phone)

            SectionLabel("Seguridad")
            FormInput(label = "Contraseña", placeholder = "Ingresa una contraseña", isPassword = true)
            FormInput(label = "Confirmar contraseña", placeholder = "Repite tu contraseña", isPassword = true)

            Spacer(Modifier.height(24.dp))
            TermsCheckbox(checked = acceptedTerms, onCheckedChange = { acceptedTerms = it })
            PrimaryButton(
                text = "Crear perfil",
                onClick = {
                    // TODO: supabase.auth.signUp(email, password) e insertar en public.cuentas
                    //  (tipo_cuenta = 'particular', nombre, apellido) y en public.telefonos_contacto
                    if (acceptedTerms) onCreated()
                },
                modifier = Modifier.alpha(if (acceptedTerms) 1f else 0.5f)
            )
            Spacer(Modifier.height(40.dp))
        }
    }
}
