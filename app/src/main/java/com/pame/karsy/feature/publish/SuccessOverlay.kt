package com.pame.karsy.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/**
 * Pantalla de éxito tras publicar ("¡Publicado!"). [onDone] lleva al panel del vendedor.
 * La publicación queda como propuesta pendiente hasta que un administrador la aprueba.
 */
@Composable
fun SuccessOverlay(
    title: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    resubmitted: Boolean = false,
    /** Se editó una publicación ya aprobada: sigue visible con la versión anterior. */
    approvedEdit: Boolean = false,
) {
    BackHandler(onBack = onDone)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KarsyNavy.copy(alpha = 0.94f))
            // Bloquea los toques hacia la pantalla de abajo.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .padding(bottom = 24.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(KarsyTeal),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(36.dp))
        }
        Text(
            stringResource(if (resubmitted) R.string.publish_resubmit_success_title else R.string.publish_success_title),
            fontFamily = Outfit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyWhite,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            stringResource(
                when {
                    approvedEdit -> R.string.publish_edit_success_message
                    resubmitted -> R.string.publish_resubmit_success_message
                    else -> R.string.publish_success_message
                },
                title
            ),
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyWhite.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(start = 40.dp, end = 40.dp, bottom = 32.dp)
        )
        Button(
            onClick = onDone,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = KarsyTeal, contentColor = KarsyWhite),
            contentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)
        ) {
            Text(stringResource(R.string.publish_success_go_dashboard), fontFamily = Outfit, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
