package com.pame.karsy.feature.profile

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pame.karsy.R
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Correo de soporte de Karsy (también en la página Ayuda de la web). */
private const val CORREO_SOPORTE = "karsypi5@gmail.com"

/** Preguntas frecuentes: pregunta y respuesta. Las mismas que en la web (Ayuda.tsx). */
private val PREGUNTAS: List<Pair<Int, Int>> = listOf(
    R.string.help_q_publish to R.string.help_a_publish,
    R.string.help_q_review to R.string.help_a_review,
    R.string.help_q_rejected to R.string.help_a_rejected,
    R.string.help_q_featured to R.string.help_a_featured,
    R.string.help_q_featured_cost to R.string.help_a_featured_cost,
    R.string.help_q_verify to R.string.help_a_verify,
    R.string.help_q_contact to R.string.help_a_contact,
    R.string.help_q_safe to R.string.help_a_safe,
    R.string.help_q_report to R.string.help_a_report,
    R.string.help_q_language to R.string.help_a_language,
)

/** Ayuda (Ajustes → Ayuda): preguntas frecuentes que se abren al tocarlas y el correo de soporte. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    // Índice de la pregunta abierta (una a la vez); -1 = ninguna.
    var abierta by rememberSaveable { mutableIntStateOf(-1) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = stringResource(R.string.help_title),
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
        ) {
            Text(stringResource(R.string.help_sub), fontFamily = DmSans, fontSize = 14.sp, color = KarsyMid)
            Spacer(Modifier.height(14.dp))
            ProfileCard {
                PREGUNTAS.forEachIndexed { i, (pregunta, respuesta) ->
                    if (i > 0) RowDivider()
                    Pregunta(pregunta, respuesta, abierta == i) { abierta = if (abierta == i) -1 else i }
                }
            }
            Spacer(Modifier.height(16.dp))
            ProfileCard {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.help_contact_title), fontFamily = Outfit, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                    Text(stringResource(R.string.help_contact_desc), fontFamily = DmSans, fontSize = 14.sp, lineHeight = 20.sp, color = KarsyCharcoal)
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_SENDTO, "mailto:$CORREO_SOPORTE".toUri()))
                            } catch (e: ActivityNotFoundException) {
                                // Sin app de correo: se muestra la dirección para copiarla a mano.
                                Toast.makeText(context, CORREO_SOPORTE, Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyTeal, contentColor = KarsyWhite),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Outlined.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(CORREO_SOPORTE, fontFamily = DmSans, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Pregunta(@StringRes pregunta: Int, @StringRes respuesta: Int, abierta: Boolean, onToggle: () -> Unit) {
    // El lector de pantalla anuncia si se abre o se cierra la respuesta.
    val accion = stringResource(if (abierta) R.string.help_hide_answer else R.string.help_show_answer)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = accion, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(pregunta),
                fontFamily = DmSans,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = KarsyInk,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = KarsyMid,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(if (abierta) 180f else 0f)
            )
        }
        AnimatedVisibility(visible = abierta) {
            Text(
                stringResource(respuesta),
                fontFamily = DmSans,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = KarsyCharcoal,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}
