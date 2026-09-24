package com.pame.karsy.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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

private val OtpBorder = Color(0xFFDDE5E9)

/**
 * Inicio de sesión + flujo de recuperación de contraseña.
 * recoveryStep: 0 = formulario de login, 1 = correo, 2 = código, 3 = nueva contraseña.
 */
@Composable
fun LoginScreen(onBack: () -> Unit, onLogin: (SessionAccount) -> Unit, onRegister: () -> Unit) {
    var recoveryStep by rememberSaveable { mutableIntStateOf(0) }

    if (recoveryStep == 0) {
        LoginForm(
            onBack = onBack,
            onLogin = onLogin,
            onRegister = onRegister,
            onForgot = { recoveryStep = 1 }
        )
    } else {
        RecoveryFlow(
            step = recoveryStep,
            onStepChange = { recoveryStep = it }
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Formulario de login
// ---------------------------------------------------------------------------------------------

@Composable
private fun LoginForm(
    onBack: () -> Unit,
    onLogin: (SessionAccount) -> Unit,
    onRegister: () -> Unit,
    onForgot: () -> Unit,
    vm: LoginViewModel = viewModel(),
) {
    var email by rememberSaveable { mutableStateOf("") }
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
                            .clickable(onClick = onForgot)
                            .padding(4.dp)
                    )
                }
                Spacer(Modifier.height(28.dp))

                // El destino (admin / lote / particular) lo decide la cuenta en Supabase.
                PrimaryButton(
                    text = if (vm.loading) "Iniciando sesión…" else "Iniciar Sesión",
                    enabled = !vm.loading,
                    onClick = { vm.signIn(email, password, onLogin) }
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
private fun RecoveryFlow(step: Int, onStepChange: (Int) -> Unit) {
    var recoveryEmail by rememberSaveable { mutableStateOf("") }
    val otp = remember { mutableStateListOf("", "", "", "") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var success by rememberSaveable { mutableStateOf(false) }

    val goBack = { onStepChange(step - 1) }
    BackHandler(onBack = goBack)

    val maskedEmail = run {
        val parts = recoveryEmail.split("@")
        if (parts.size < 2 || parts[1].isEmpty()) {
            "al******@gmail.com"
        } else {
            val name = parts[0]
            val visible = name.take(2)
            visible + "*".repeat(maxOf(5, name.length - visible.length)) + "@" + parts[1]
        }
    }

    val title = when (step) {
        1 -> "¿Olvidaste tu contraseña?"
        2 -> "Código de verificación"
        else -> "Nueva contraseña"
    }
    val subtitle = when (step) {
        1 -> "Ingresa tu correo electrónico asociado a tu cuenta para enviarte un código de verificación."
        2 -> "Ingresa el código de 4 dígitos enviado a tu correo $maskedEmail."
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
                IconButton(onClick = goBack, modifier = Modifier.align(Alignment.CenterStart)) {
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
                        val valid = recoveryEmail.isNotBlank() && recoveryEmail.contains("@")
                        IconInput(
                            label = "CORREO ELECTRÓNICO",
                            value = recoveryEmail,
                            onValueChange = { recoveryEmail = it },
                            placeholder = "ejemplo@correo.com",
                            leading = Icons.Outlined.MailOutline,
                            keyboardType = KeyboardType.Email,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        PrimaryButton(
                            text = "Enviar código",
                            onClick = {
                                // TODO: supabase.auth.resetPasswordForEmail(recoveryEmail) / enviar OTP al correo
                                if (valid) onStepChange(2)
                            },
                            modifier = Modifier.alpha(if (valid) 1f else 0.55f)
                        )
                    }

                    2 -> {
                        val complete = otp.all { it.isNotEmpty() }
                        OtpRow(otp = otp)
                        Spacer(Modifier.height(22.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("¿No recibiste el código? ", fontFamily = DmSans, fontSize = 12.5.sp, color = KarsyMid)
                            Text(
                                "Reenviar código",
                                fontFamily = DmSans,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyTeal,
                                modifier = Modifier.clickable {
                                    // TODO: volver a enviar el código de verificación al correo
                                    for (i in otp.indices) otp[i] = ""
                                }
                            )
                        }
                        Spacer(Modifier.height(28.dp))
                        PrimaryButton(
                            text = "Verificar",
                            onClick = {
                                // TODO: supabase.auth.verifyEmailOtp(type = RECOVERY, email, token)
                                if (complete) onStepChange(3)
                            },
                            modifier = Modifier.alpha(if (complete) 1f else 0.55f)
                        )
                    }

                    else -> {
                        // Sin reglas de contraseña: solo debe coincidir con la confirmación.
                        val valid = newPassword.isNotEmpty() && newPassword == confirmPassword
                        IconInput(
                            label = "NUEVA CONTRASEÑA",
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            placeholder = "••••••••••••",
                            leading = Icons.Outlined.Lock,
                            isPassword = true,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        IconInput(
                            label = "CONFIRMAR CONTRASEÑA",
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "••••••••••••",
                            leading = Icons.Outlined.Lock,
                            isPassword = true,
                            modifier = Modifier.padding(bottom = 22.dp)
                        )
                        PrimaryButton(
                            text = "Restablecer contraseña",
                            onClick = {
                                // TODO: supabase.auth.updateUser { password = newPassword }
                                if (valid) success = true
                            },
                            modifier = Modifier.alpha(if (valid) 1f else 0.55f)
                        )
                        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
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
            }
        }

        if (success) {
            SuccessOverlay(onLogin = {
                success = false
                onStepChange(0)
            })
        }
    }
}

/** 4 casillas del código de verificación; avanzan/retroceden el foco solas. */
@Composable
private fun OtpRow(otp: MutableList<String>) {
    val focusers = remember { List(otp.size) { FocusRequester() } }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        otp.forEachIndexed { index, digit ->
            val shape = RoundedCornerShape(14.dp)
            BasicTextField(
                value = digit,
                onValueChange = { raw ->
                    val v = raw.filter { it.isDigit() }.takeLast(1)
                    otp[index] = v
                    if (v.isNotEmpty() && index < otp.lastIndex) focusers[index + 1].requestFocus()
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                textStyle = TextStyle(
                    fontFamily = Outfit,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyNavy,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(KarsyTeal),
                modifier = Modifier
                    .size(width = 56.dp, height = 58.dp)
                    .clip(shape)
                    .background(KarsyBg)
                    .border(if (digit.isNotEmpty()) 1.8.dp else 1.5.dp, if (digit.isNotEmpty()) KarsyTeal else OtpBorder, shape)
                    .focusRequester(focusers[index])
                    .onPreviewKeyEvent {
                        if (it.type == KeyEventType.KeyDown && it.key == Key.Backspace && otp[index].isEmpty() && index > 0) {
                            focusers[index - 1].requestFocus()
                            true
                        } else false
                    },
                decorationBox = { inner ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { inner() }
                }
            )
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
