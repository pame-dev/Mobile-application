package com.pame.karsy.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Códigos que Supabase Auth manda por correo (verificar correo y restablecer contraseña).

/** Debe coincidir con "Email OTP Length" en Supabase (Authentication → Sign In / Providers → Email). */
const val EMAIL_CODE_LENGTH = 6

/** Espera para pedir otro código: Supabase no manda otro correo al mismo usuario antes de 60 s. */
class ResendCountdown(private val scope: CoroutineScope) {
    var seconds by mutableIntStateOf(0)
        private set
    private var job: Job? = null

    fun start() {
        job?.cancel()
        job = scope.launch {
            for (s in SECONDS downTo 1) {
                seconds = s
                delay(1_000)
            }
            seconds = 0
        }
    }

    private companion object {
        const val SECONDS = 60
    }
}

/** "¿No recibiste el código? Reenviar código", o la espera mientras no se puede pedir otro. */
@Composable
fun ResendCodeRow(sending: Boolean, secondsLeft: Int, onResend: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            sending -> Text(stringResource(R.string.auth_sending_code), fontFamily = DmSans, fontSize = 12.5.sp, color = KarsyMid)
            secondsLeft > 0 -> Text(
                stringResource(R.string.auth_resend_wait, secondsLeft),
                fontFamily = DmSans,
                fontSize = 12.5.sp,
                color = KarsyMid
            )
            else -> {
                Text(stringResource(R.string.auth_no_code_question), fontFamily = DmSans, fontSize = 12.5.sp, color = KarsyMid)
                Text(
                    stringResource(R.string.auth_resend_code),
                    fontFamily = DmSans,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyTeal,
                    modifier = Modifier.clickable(onClick = onResend)
                )
            }
        }
    }
}
