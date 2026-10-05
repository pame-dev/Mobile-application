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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SectionLabel
import com.pame.karsy.core.components.TermsCheckbox
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.theme.KarsyBg

/** Formulario de registro de una cuenta Particular. Crea el usuario en Supabase Auth. */
@Composable
fun RegisterParticularScreen(
    onBack: () -> Unit,
    onCreated: (SessionAccount) -> Unit,
    onVerifyEmail: (String) -> Unit,
    onTerms: () -> Unit = {},
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
            title = stringResource(R.string.auth_create_profile),
            subtitle = stringResource(R.string.auth_particular_subtitle)
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        ) {
            SectionLabel(stringResource(R.string.auth_section_personal_info))
            FormInput(
                label = stringResource(R.string.auth_first_name), placeholder = stringResource(R.string.auth_first_name_placeholder),
                value = vm.nombre, onValueChange = { vm.nombre = it }, error = vm.errorFor("nombre")
            )
            FormInput(
                label = stringResource(R.string.auth_last_name), placeholder = stringResource(R.string.auth_last_name_placeholder),
                value = vm.apellido, onValueChange = { vm.apellido = it }, error = vm.errorFor("apellido")
            )
            FormInput(
                label = stringResource(R.string.auth_email), placeholder = stringResource(R.string.auth_register_email_placeholder), keyboardType = KeyboardType.Email,
                value = vm.correo, onValueChange = { vm.correo = it }, error = vm.errorFor("correo")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = stringResource(R.string.auth_city), placeholder = stringResource(R.string.auth_city_placeholder), modifier = Modifier.weight(1f),
                    value = vm.ciudad, onValueChange = { vm.ciudad = it }, error = vm.errorFor("ciudad")
                )
                FormInput(
                    label = stringResource(R.string.auth_state), placeholder = stringResource(R.string.auth_state_placeholder), modifier = Modifier.weight(1f),
                    value = vm.estado, onValueChange = { vm.estado = it }, error = vm.errorFor("estado")
                )
            }

            SectionLabel(stringResource(R.string.auth_section_contact))
            ContactFields(vm, phoneLabel = stringResource(R.string.auth_phone))

            SectionLabel(stringResource(R.string.auth_section_security))
            FormInput(
                label = stringResource(R.string.auth_password), placeholder = stringResource(R.string.auth_password_placeholder), isPassword = true,
                value = vm.password, onValueChange = { vm.password = it }, error = vm.errorFor("password")
            )
            FormInput(
                label = stringResource(R.string.auth_confirm_password), placeholder = stringResource(R.string.auth_confirm_password_placeholder), isPassword = true,
                value = vm.confirmPassword, onValueChange = { vm.confirmPassword = it }, error = vm.passwordMismatch
            )

            Spacer(Modifier.height(24.dp))
            TermsCheckbox(checked = vm.acceptedTerms, onCheckedChange = { vm.acceptedTerms = it }, onOpenTerms = onTerms)
            PrimaryButton(
                text = if (vm.loading) stringResource(R.string.auth_creating_profile) else stringResource(R.string.auth_create_profile),
                enabled = !vm.loading,
                onClick = { vm.submit(esLote = false, context = context, onCreated = onCreated, onVerifyEmail = onVerifyEmail) },
                modifier = Modifier.alpha(if (vm.acceptedTerms) 1f else 0.5f)
            )
            RegisterError(vm.error)
            Spacer(Modifier.height(40.dp))
        }
    }
}
