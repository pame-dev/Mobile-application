package com.pame.karsy.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.components.OtpInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.karsyTextFieldColors
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Formato

/**
 * Inicio de sesión + flujo de recuperación de contraseña (pasos en [PasswordRecoveryViewModel.step]).
 * Si el correo de la cuenta no está verificado se llama a [onVerifyEmail].
 */
@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: (SessionAccount) -> Unit,
    onVerifyEmail: (String) -> Unit,
    onRegister: () -> Unit,
    recovery: PasswordRecoveryViewModel = viewModel(),
) {
    if (recovery.step == 0) {
        LoginForm(
            initialEmail = recovery.email,
            onBack = onBack,
            onLogin = onLogin,
            onVerifyEmail = onVerifyEmail,
            onRegister = onRegister,
            onForgot = recovery::start
        )
    } else {
        RecoveryFlow(recovery)
    }
}

// ---------------------------------------------------------------------------------------------
// Formulario de login
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoginForm(
    initialEmail: String,
    onBack: () -> Unit,
    onLogin: (SessionAccount) -> Unit,
    onVerifyEmail: (String) -> Unit,
    onRegister: () -> Unit,
    onForgot: (String) -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    // Al volver de "Olvidé mi contraseña" se conserva el correo que se usó ahí.
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 12.dp, top = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Regresar", tint = KarsyNavy, modifier = Modifier.size(28.dp))
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KarsyLogo(size = 52.dp)
                Spacer(Modifier.height(24.dp))
                Text(
                    "Iniciar Sesión",
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    letterSpacing = (-0.02).em,
                    color = KarsyCharcoal
                )
            }

            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 28.dp)) {
                IconInput(
                    label = "CORREO ELECTRÓNICO",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "ejemplo@correo.com",
                    leading = Icons.Outlined.MailOutline,
                    keyboardType = KeyboardType.Email,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                IconInput(
                    label = "CONTRASEÑA",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "••••••••••••",
                    leading = Icons.Outlined.Lock,
                    isPassword = true,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        "¿Olvidaste tu contraseña?",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KarsyTeal,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onForgot(email) }
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.height(28.dp))

                // El destino (admin / lote / particular) lo decide la cuenta en Supabase.
                PrimaryButton(
                    text = if (vm.loading) "Iniciando sesión…" else "Iniciar Sesión",
                    enabled = !vm.loading,
                    onClick = { vm.signIn(email, password, onLogin, onVerifyEmail) }
                )
                vm.error?.let {
                    Text(
                        it,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = KarsyError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 16.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("¿Aún no tienes cuenta? ", fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal)
            Text(
                "Registrarse",
                fontFamily = DmSans,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTeal,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onRegister)
            )
        }
    }
}

/** Campo con etiqueta en mayúsculas e ícono a la izquierda (inputs del login del mockup). */
@Composable
private fun IconInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: ImageVector,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var show by remember { mutableStateOf(false) }
    Column(modifier) {
        Text(
            label,
            fontFamily = DmSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.04.em,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid) },
            leadingIcon = { Icon(leading, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(20.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { show = !show }) {
                        Icon(
                            if (show) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (show) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = KarsyMid,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            textStyle = TextStyle(fontFamily = DmSans, fontSize = 15.sp, color = KarsyCharcoal),
            visualTransformation = if (isPassword && !show) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            shape = RoundedCornerShape(12.dp),
            colors = karsyTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Recuperación de contraseña
// ---------------------------------------------------------------------------------------------

@Composable
private fun RecoveryFlow(vm: PasswordRecoveryViewModel) {
    val step = vm.step
    BackHandler(onBack = vm::back)

    val title = when (step) {
        1 -> "¿Olvidaste tu contraseña?"
        2 -> "Código de verificación"
        else -> "Nueva contraseña"
    }
    val subtitle = when (step) {
        1 -> "Ingresa tu correo electrónico asociado a tu cuenta para enviarte un código de verificación."
        2 -> "Ingresa el código de $EMAIL_CODE_LENGTH dígitos enviado a tu correo " +
            "${Formato.correoOculto(vm.email) ?: vm.email}. Si no lo ves, revisa tu carpeta de spam."
        else -> "Crea una contraseña segura y fácil de recordar para acceder a tu cuenta."
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // Encabezado: flecha atrás + logo centrado
            Box(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 12.dp, end = 24.dp, top = 8.dp)
            ) {
                IconButton(onClick = vm::back, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Regresar", tint = KarsyNavy, modifier = Modifier.size(28.dp))
                }
                KarsyLogo(size = 46.dp, modifier = Modifier.align(Alignment.Center))
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 28.dp, top = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    title,
                    fontFamily = Outfit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 25.sp,
                    lineHeight = 30.sp,
                    color = KarsyNavy,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(9.dp))
                Text(
                    subtitle,
                    fontFamily = DmSans,
                    fontSize = 13.5.sp,
                    lineHeight = 21.sp,
                    color = KarsyMid,
                    textAlign = TextAlign.Center
                )
            }

            Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 36.dp)) {
                when (step) {
                    1 -> {
                        val valid = vm.email.isNotBlank() && vm.email.contains("@")
                        IconInput(
                            label = "CORREO ELECTRÓNICO",
                            value = vm.email,
                            onValueChange = { vm.email = it },
                            placeholder = "ejemplo@correo.com",
                            leading = Icons.Outlined.MailOutline,
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        PrimaryButton(
                            text = if (vm.sending) "Enviando código…" else "Enviar código",
                            enabled = !vm.sending,
                            onClick = vm::sendCode,
                            modifier = Modifier.alpha(if (valid) 1f else 0.55f)
                        )
                    }

                    2 -> {
                        val complete = vm.code.all { it.isNotEmpty() }
                        OtpInput(otp = vm.code, autoFocus = true)
                        Spacer(Modifier.height(22.dp))
                        ResendCodeRow(sending = vm.sending, secondsLeft = vm.countdown.seconds, onResend = vm::resend)
                        Spacer(Modifier.height(28.dp))
                        PrimaryButton(
                            text = if (vm.loading) "Verificando…" else "Verificar",
                            enabled = !vm.loading,
                            onClick = vm::verifyCode,
                            modifier = Modifier.alpha(if (complete) 1f else 0.55f)
                        )
                    }

                    else -> {
                        // Supabase pide mínimo 6 caracteres; aquí solo se revisa que coincidan.
                        val valid = vm.newPassword.isNotEmpty() && vm.newPassword == vm.confirmPassword
                        IconInput(
                            label = "NUEVA CONTRASEÑA",
                            value = vm.newPassword,
                            onValueChange = { vm.newPassword = it },
                            placeholder = "••••••••••••",
                            leading = Icons.Outlined.Lock,
                            isPassword = true,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        IconInput(
                            label = "CONFIRMAR CONTRASEÑA",
                            value = vm.confirmPassword,
                            onValueChange = { vm.confirmPassword = it },
                            placeholder = "••••••••••••",
                            leading = Icons.Outlined.Lock,
                            isPassword = true,
                            modifier = Modifier.padding(bottom = 22.dp)
                        )
                        PrimaryButton(
                            text = if (vm.loading) "Guardando…" else "Restablecer contraseña",
                            enabled = !vm.loading,
                            onClick = vm::resetPassword,
                            modifier = Modifier.alpha(if (valid) 1f else 0.55f)
                        )
                        if (vm.passwordMismatch) {
                            Text(
                                "Las contraseñas no coinciden.",
                                fontFamily = DmSans,
                                fontSize = 11.5.sp,
                                color = KarsyError,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            )
                        }
                    }
                }
                vm.error?.let {
                    Text(
                        it,
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        color = KarsyError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        if (vm.success) {
            SuccessOverlay(onLogin = vm::finish)
        }
    }
}

@Composable
private fun SuccessOverlay(onLogin: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(KarsyNavy.copy(alpha = 0.5f))
            // Bloquea los toques al contenido de atrás
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(20.dp, shape)
                .clip(shape)
                .background(KarsyWhite)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(KarsyTeal),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(17.dp))
            Text(
                "¡Contraseña actualizada!",
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = KarsyNavy,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Tu contraseña se ha cambiado con éxito. Ya puedes iniciar sesión con tu nueva credencial.",
                fontFamily = DmSans,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = KarsyCharcoal,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(22.dp))
            PrimaryButton(text = "Iniciar Sesión", onClick = onLogin)
        }
    }
}
