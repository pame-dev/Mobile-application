package com.pame.karsy.feature.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.TermsCheckbox
import com.pame.karsy.core.theme.KarsyBg

/** Formulario de registro de una cuenta Particular. Crea el usuario en Supabase Auth. */
@Composable
fun RegisterParticularScreen(
    onBack: () -> Unit,
    onCreated: (SessionAccount) -> Unit,
    vm: RegisterViewModel = viewModel(),
) {
    val context = LocalContext.current

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
            SectionLabel("Información personal")
            FormInput(
                label = "Nombre", placeholder = "Ingresa tu nombre",
                value = vm.nombre, onValueChange = { vm.nombre = it }, error = vm.errorFor("nombre")
            )
            FormInput(
                label = "Apellido", placeholder = "Ingresa tu apellido",
                value = vm.apellido, onValueChange = { vm.apellido = it }, error = vm.errorFor("apellido")
            )
            FormInput(
                label = "Correo electrónico", placeholder = "correo@ejemplo.com", keyboardType = KeyboardType.Email,
                value = vm.correo, onValueChange = { vm.correo = it }, error = vm.errorFor("correo")
            )
            FormInput(
                label = "Teléfono", placeholder = "10 dígitos", keyboardType = KeyboardType.Phone,
                value = vm.telefono, onValueChange = { vm.telefono = it }
            )
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
                onClick = { vm.submit(esLote = false, context = context, onCreated = onCreated) },
                modifier = Modifier.alpha(if (vm.acceptedTerms) 1f else 0.5f)
            )
            RegisterError(vm.error)
            Spacer(Modifier.height(40.dp))
        }
    }
}
