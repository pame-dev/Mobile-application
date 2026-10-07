package com.pame.karsy.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.util.PasswordRules

/** Lista de requisitos de la contraseña; cada uno se marca en cuanto se cumple. */
@Composable
fun PasswordRequirements(password: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PasswordRules.Rule.entries.forEach { rule ->
            val ok = rule.cumple(password)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (ok) KarsyTeal else KarsyMid,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    stringResource(rule.label),
                    fontFamily = DmSans,
                    fontSize = 12.sp,
                    color = if (ok) KarsyTeal else KarsyMid,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
