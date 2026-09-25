package com.pame.karsy.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.core.components.KarsyLogo
import com.pame.karsy.core.components.OtpInput
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.session.SessionAccount
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.Formato

/**
 * Pide el código que Supabase manda al correo; al verificarlo se abre la sesión.
 * [sendCode] = true genera un código nuevo al entrar (viene del login).
 */
@Composable
fun VerifyEmailScreen(
    email: String,
    sendCode: Boolean,
    onBack: () -> Unit,
    onVerified: (SessionAccount) -> Unit,
    vm: VerifyEmailViewModel = viewModel(),
) {
    LaunchedEffect(Unit) { vm.start(email, sendCode) }
    val complete = vm.code.all { it.isNotEmpty() }

    Column(
        Modifier
            .fillMaxSize()
            .background(KarsyBg)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
    ) {
        // Encabezado: flecha atrás + logo centrado (igual que la recuperación de contraseña)
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 24.dp, top = 8.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
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
                "Verifica tu correo",
                fontFamily = Outfit,
                fontWeight = FontWeight.Bold,
                fontSize = 25.sp,
                lineHeight = 30.sp,
                color = KarsyNavy,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Ingresa el código de $EMAIL_CODE_LENGTH dígitos que enviamos a " +
                    "${Formato.correoOculto(email) ?: email}. Si no lo ves, revisa tu carpeta de spam.",
                fontFamily = DmSans,
                fontSize = 13.5.sp,
                lineHeight = 21.sp,
                color = KarsyMid,
                textAlign = TextAlign.Center
            )
        }

        Column(Modifier.padding(start = 28.dp, end = 28.dp, top = 32.dp, bottom = 36.dp)) {
            OtpInput(otp = vm.code, autoFocus = true)
            Spacer(Modifier.height(22.dp))
            ResendCodeRow(sending = vm.sending, secondsLeft = vm.countdown.seconds, onResend = { vm.resend(email) })
            Spacer(Modifier.height(28.dp))
            PrimaryButton(
                text = if (vm.verifying) "Verificando…" else "Verificar",
                enabled = !vm.verifying,
                onClick = { vm.verify(email, onVerified) },
                modifier = Modifier.alpha(if (complete) 1f else 0.55f)
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
}
