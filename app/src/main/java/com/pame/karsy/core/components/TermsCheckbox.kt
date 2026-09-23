package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyCheckBorder
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyWhite

/** Casilla "Acepto los términos y condiciones de KARSY" (TermsNote del mockup). */
@Composable
fun TermsCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .clip(shape)
            .background(if (checked) KarsyTealLight else KarsyBg)
            .border(1.5.dp, if (checked) KarsyTeal else KarsyBorder, shape)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) KarsyTeal else KarsyWhite)
                .border(2.dp, if (checked) KarsyTeal else KarsyCheckBorder, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            text = buildAnnotatedString {
                append("Acepto los ")
                withStyle(SpanStyle(color = KarsyTeal, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)) {
                    append("términos y condiciones")
                }
                append(" de KARSY.")
            },
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = KarsyCharcoal
        )
    }
}
