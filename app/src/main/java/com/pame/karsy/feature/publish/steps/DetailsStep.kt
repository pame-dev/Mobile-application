package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.feature.publish.PublishStepScaffold
import com.pame.karsy.feature.publish.PublishViewModel

/** Paso 3: descripción e imperfecciones. */
@Composable
fun DetailsStep(form: PublishViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    PublishStepScaffold(
        step = 3,
        onBack = onBack,
        buttonText = stringResource(R.string.publish_next_confirmation),
        onButtonClick = onNext
    ) {
        form.error?.let { StepError(it) }
        val tipBold = stringResource(R.string.publish_tip_bold, 60)
        val tipFull = stringResource(R.string.publish_tip_full, tipBold)
        LimitedTextArea(
            title = stringResource(R.string.publish_description_title),
            value = form.descripcion,
            onValueChange = { form.descripcion = it },
            placeholder = stringResource(R.string.publish_description_placeholder),
            maxChars = 500
        )
        LimitedTextArea(
            title = stringResource(R.string.publish_imperfections_title),
            value = form.imperfecciones,
            onValueChange = { form.imperfecciones = it },
            placeholder = stringResource(R.string.publish_imperfections_placeholder),
            maxChars = 300
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KarsyTealSoft)
                .border(1.dp, KarsyTeal.copy(alpha = 0.13f), RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(KarsyTeal),
                contentAlignment = Alignment.Center
            ) {
                Text("i", fontFamily = DmSans, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KarsyWhite)
            }
            Text(
                text = buildAnnotatedString {
                    append(tipFull)
                    val boldStart = tipFull.indexOf(tipBold)
                    if (boldStart >= 0) {
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), boldStart, boldStart + tipBold.length)
                    }
                },
                fontFamily = DmSans,
                fontSize = 12.sp,
                lineHeight = 19.sp,
                color = KarsyNavy
            )
        }
    }
}

@Composable
private fun LimitedTextArea(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxChars: Int,
) {
    Column {
        Text(
            title,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(KarsyWhite)
                .border(1.dp, KarsyBorderMuted, RoundedCornerShape(16.dp))
        ) {
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(maxChars)) },
                minLines = 5,
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, lineHeight = 19.sp, color = KarsyCharcoal),
                cursorBrush = SolidColor(KarsyNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = KarsyCharcoal.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
            )
            Text(
                "${value.length} / $maxChars",
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = KarsyMid,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }
    }
}
