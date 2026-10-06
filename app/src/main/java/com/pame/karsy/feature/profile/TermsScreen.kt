package com.pame.karsy.feature.profile

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.Outfit

/** Sección de un documento legal (título + texto). */
internal data class LegalSection(@StringRes val titleRes: Int, @StringRes val bodyRes: Int)

private val termsSections = listOf(
    LegalSection(R.string.terms_s1_title, R.string.terms_s1_body),
    LegalSection(R.string.terms_s2_title, R.string.terms_s2_body),
    LegalSection(R.string.terms_s3_title, R.string.terms_s3_body),
    LegalSection(R.string.terms_s4_title, R.string.terms_s4_body),
    LegalSection(R.string.terms_s5_title, R.string.terms_s5_body),
    LegalSection(R.string.terms_s6_title, R.string.terms_s6_body),
    LegalSection(R.string.terms_s7_title, R.string.terms_s7_body),
    LegalSection(R.string.terms_s8_title, R.string.terms_s8_body),
    LegalSection(R.string.terms_s9_title, R.string.terms_s9_body),
)

/** Términos y condiciones, abierto desde Configuración y desde los registros. */
@Composable
fun TermsScreen(onBack: () -> Unit) = LegalDocumentScreen(
    titleRes = R.string.terms_title,
    lastUpdateRes = R.string.terms_last_update,
    introRes = R.string.terms_intro,
    sections = termsSections,
    onBack = onBack,
)

/**
 * Documento legal con fecha, introducción y secciones (Términos, Aviso de privacidad).
 * Mismo layout que Historial y Configuración.
 */
@Composable
internal fun LegalDocumentScreen(
    @StringRes titleRes: Int,
    @StringRes lastUpdateRes: Int,
    @StringRes introRes: Int,
    sections: List<LegalSection>,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = stringResource(titleRes),
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
            ProfileCard {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Text(
                        stringResource(lastUpdateRes),
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        color = KarsyMid
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        stringResource(introRes),
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = KarsyCharcoal
                    )
                }
                sections.forEach { section ->
                    RowDivider()
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
                        Text(
                            stringResource(section.titleRes),
                            fontFamily = Outfit,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = KarsyInk
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(section.bodyRes),
                            fontFamily = DmSans,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = KarsyCharcoal
                        )
                    }
                }
            }
        }
    }
}
