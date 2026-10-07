package com.pame.karsy.feature.register

import androidx.compose.foundation.background
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import com.pame.karsy.core.util.FormRules
import com.pame.karsy.core.components.EstadoMunicipioPickers
import com.pame.karsy.core.components.BackTopBar
import com.pame.karsy.core.components.FormInput
import com.pame.karsy.core.components.PasswordRequirements
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
    onLogin: () -> Unit = {},
    vm: RegisterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scroller = rememberErrorScroller(vm, esLote = false)

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
                .then(scroller.viewport)
                .verticalScroll(scroller.scrollState)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        ) {
            SectionLabel(stringResource(R.string.auth_section_personal_info))
            FormInput(
                label = stringResource(R.string.auth_first_name), placeholder = stringResource(R.string.auth_first_name_placeholder),
                value = vm.nombre, onValueChange = { vm.nombre = it }, inputFilter = FormRules::nombreAlEscribir, error = vm.errorFor("nombre"), onFocusLost = { vm.touch("nombre") },
                capitalization = KeyboardCapitalization.Words, onPositioned = scroller.mark("nombre")
            )
            FormInput(
                label = stringResource(R.string.auth_last_name), placeholder = stringResource(R.string.auth_last_name_placeholder),
                value = vm.apellido, onValueChange = { vm.apellido = it }, inputFilter = FormRules::nombreAlEscribir, error = vm.errorFor("apellido"), onFocusLost = { vm.touch("apellido") },
                capitalization = KeyboardCapitalization.Words, onPositioned = scroller.mark("apellido")
            )
            FormInput(
                label = stringResource(R.string.auth_email), placeholder = stringResource(R.string.auth_register_email_placeholder), keyboardType = KeyboardType.Email,
                value = vm.correo, onValueChange = { vm.correo = it }, error = vm.errorFor("correo"), onFocusLost = { vm.touch("correo") },
                onPositioned = scroller.mark("correo")
            )
            EmailExistsAction(vm, onLogin)
            EmailSuggestion(vm)
            // Estado y municipio de México (catálogo del INEGI): el municipio depende del estado.
            EstadoMunicipioPickers(
                estado = vm.estado,
                municipio = vm.ciudad,
                onEstado = { vm.estado = it },
                onMunicipio = { vm.ciudad = it },
                estadoError = vm.errorFor("estado"),
                municipioError = vm.errorFor("ciudad"),
                modifier = scroller.markModifier("estado", "ciudad")
            )

            SectionLabel(stringResource(R.string.auth_section_contact))
            ContactFields(vm, phoneLabel = stringResource(R.string.auth_phone), onPositioned = { f, y -> scroller.mark(f)(y) })

            SectionLabel(stringResource(R.string.auth_section_security))
            FormInput(
                label = stringResource(R.string.auth_password), placeholder = stringResource(R.string.auth_password_placeholder), isPassword = true,
                value = vm.password, onValueChange = { vm.password = it }, error = vm.errorFor("password"), onFocusLost = { vm.touch("password") },
                onPositioned = scroller.mark("password")
            )
            PasswordRequirements(vm.password, Modifier.padding(start = 4.dp, bottom = 14.dp))
            FormInput(
                label = stringResource(R.string.auth_confirm_password), placeholder = stringResource(R.string.auth_confirm_password_placeholder), isPassword = true,
                value = vm.confirmPassword, onValueChange = { vm.confirmPassword = it }, error = vm.passwordMismatch,
                onPositioned = scroller.mark("confirmPassword")
            )

            Spacer(Modifier.height(24.dp))
            TermsCheckbox(
                checked = vm.acceptedTerms, onCheckedChange = { vm.acceptedTerms = it }, onOpenTerms = onTerms,
                error = vm.termsError, modifier = scroller.markModifier("terminos")
            )
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
