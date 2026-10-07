package com.pame.karsy.feature.register

import android.net.Uri
import com.pame.karsy.core.theme.KarsyError
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
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
    onTerms: () -> Unit = {},
    onLogin: () -> Unit = {},
    vm: RegisterViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scroller = rememberErrorScroller(vm, esLote = true)
    val pickLogo = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Revisa que sea imagen y de buen tamaño antes de aceptarla.
        if (uri != null) vm.elegirLogo(context, uri)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
    ) {
        BackTopBar(onBack = onBack)
        RegisterHeader(
            title = stringResource(R.string.auth_lote_title),
            subtitle = stringResource(R.string.auth_lote_subtitle)
        )
        Column(
            Modifier
                .weight(1f)
                .then(scroller.viewport)
                .verticalScroll(scroller.scrollState)
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 4.dp)
        ) {
            SectionLabel(stringResource(R.string.auth_section_manager))
            FormInput(
                label = stringResource(R.string.auth_manager_first_name), placeholder = stringResource(R.string.auth_manager_first_name_placeholder),
                value = vm.nombre, onValueChange = { vm.nombre = it }, inputFilter = FormRules::nombreAlEscribir, error = vm.errorFor("nombre"), onFocusLost = { vm.touch("nombre") },
                capitalization = KeyboardCapitalization.Words, onPositioned = scroller.mark("nombre")
            )
            FormInput(
                label = stringResource(R.string.auth_manager_last_name), placeholder = stringResource(R.string.auth_manager_last_name_placeholder),
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

            SectionLabel(stringResource(R.string.auth_section_contact))
            ContactFields(vm, phoneLabel = stringResource(R.string.auth_business_phone), onPositioned = { f, y -> scroller.mark(f)(y) })

            SectionLabel(stringResource(R.string.auth_section_lote_info))
            FormInput(
                label = stringResource(R.string.auth_lote_name), placeholder = stringResource(R.string.auth_lote_name_placeholder),
                value = vm.nombreLote, onValueChange = { vm.nombreLote = it }, error = vm.errorFor("nombreLote"), onFocusLost = { vm.touch("nombreLote") },
                inputFilter = FormRules.maximo(FormRules.LOTE_MAX), capitalization = KeyboardCapitalization.Words, onPositioned = scroller.mark("nombreLote")
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = stringResource(R.string.auth_street), placeholder = stringResource(R.string.auth_street_placeholder), modifier = Modifier.weight(2f),
                    value = vm.calle, onValueChange = { vm.calle = it }, error = vm.errorFor("calle"), onFocusLost = { vm.touch("calle") },
                    inputFilter = FormRules.maximo(FormRules.DIRECCION_MAX), onPositioned = scroller.mark("calle")
                )
                FormInput(
                    label = stringResource(R.string.auth_street_number), placeholder = "123", modifier = Modifier.weight(1f),
                    value = vm.numero, onValueChange = { vm.numero = it }, error = vm.errorFor("numero"), onFocusLost = { vm.touch("numero") },
                    inputFilter = FormRules.maximo(FormRules.NUMERO_MAX), onPositioned = scroller.mark("numero")
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FormInput(
                    label = stringResource(R.string.auth_neighborhood), placeholder = stringResource(R.string.auth_neighborhood_placeholder), modifier = Modifier.weight(2f),
                    value = vm.colonia, onValueChange = { vm.colonia = it }, error = vm.errorFor("colonia"), onFocusLost = { vm.touch("colonia") },
                    inputFilter = FormRules.maximo(FormRules.DIRECCION_MAX), onPositioned = scroller.mark("colonia")
                )
                FormInput(
                    label = stringResource(R.string.auth_zip_code), placeholder = "27000", keyboardType = KeyboardType.Number, modifier = Modifier.weight(1f),
                    value = vm.codigoPostal, onValueChange = { vm.codigoPostal = it }, inputFilter = { FormRules.soloDigitos(it, 5) }, error = vm.errorFor("codigoPostal"), onFocusLost = { vm.touch("codigoPostal") },
                    onPositioned = scroller.mark("codigoPostal")
                )
            }
            // Aviso (no bloquea) si el CP parece de otro estado.
            FieldNotice(vm.codigoPostalAviso)
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
            FormInput(
                label = stringResource(R.string.auth_lote_description),
                placeholder = stringResource(R.string.auth_lote_description_placeholder),
                singleLine = false,
                minLines = 4,
                value = vm.descripcion,
                onValueChange = { vm.descripcion = it },
                inputFilter = FormRules.maximo(FormRules.DESCRIPCION_MAX),
                maxChars = FormRules.DESCRIPCION_MAX
            )

            SectionLabel(stringResource(R.string.auth_section_profile_photo))
            PhotoPlaceholder(
                imageUri = vm.logo,
                onClick = {
                    pickLogo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            vm.logoError?.let {
                Text(
                    it,
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = KarsyError,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.auth_lote_logo_hint),
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            )

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
            contentDescription = stringResource(R.string.auth_lote_logo),
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
                stringResource(R.string.auth_add_photo),
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
