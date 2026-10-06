package com.pame.karsy.feature.profile

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.feature.home.LogoutConfirmDialog

private data class SettingsItem(
    val icon: String,
    @StringRes val labelRes: Int,
    @StringRes val descRes: Int,
    val isLanguage: Boolean = false,
    val isTerms: Boolean = false,
    val isNotifications: Boolean = false,
    val isPrivacy: Boolean = false,
)

private val settingsItems = listOf(
    SettingsItem("🔔", R.string.settings_notifications, R.string.settings_notifications_desc, isNotifications = true),
    SettingsItem("🔒", R.string.settings_privacy, R.string.settings_privacy_desc, isPrivacy = true),
    // La descripción del idioma se arma con languageSummary (idioma elegido).
    SettingsItem("🌐", R.string.settings_language, R.string.settings_language, isLanguage = true),
    SettingsItem("📄", R.string.settings_terms, R.string.settings_terms_desc, isTerms = true),
)

/** Colores de la tarjeta de "Cerrar sesión" del mockup. */
private val LogoutBorder = Color(0xFFFECACA)
private val LogoutRed = Color(0xFFD93025)

/** Pantalla de configuración (WebConfigView del mockup). */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onTerms: () -> Unit,
    onNotifications: () -> Unit,
    onPrivacy: () -> Unit,
) {
    val context = LocalContext.current
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var languageOpen by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KarsyBg)
        ) {
            SubHeader(
                title = stringResource(R.string.settings_title),
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
                    settingsItems.forEachIndexed { idx, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (item.isLanguage) languageOpen = true
                                    if (item.isTerms) onTerms()
                                    if (item.isNotifications) onNotifications()
                                    if (item.isPrivacy) onPrivacy()
                                }
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                item.icon,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(36.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(item.labelRes),
                                    fontFamily = DmSans,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = KarsyCharcoal
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    if (item.isLanguage) languageSummary(context) else stringResource(item.descRes),
                                    fontFamily = DmSans,
                                    fontSize = 13.sp,
                                    color = KarsyMid
                                )
                            }
                            RowChevron()
                        }
                        if (idx < settingsItems.lastIndex) RowDivider()
                    }
                }
                Spacer(Modifier.height(20.dp))
                ProfileCard(radius = 16.dp, borderColor = LogoutBorder, elevation = 0.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmLogout = true }
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("🚪", fontSize = 20.sp)
                        Text(
                            stringResource(R.string.settings_logout),
                            fontFamily = DmSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = LogoutRed
                        )
                    }
                }
            }
        }

        LogoutConfirmDialog(
            visible = confirmLogout,
            onCancel = { confirmLogout = false },
            onConfirm = {
                confirmLogout = false
                onLogout()
            }
        )
    }

    if (languageOpen) {
        LanguageSheet(
            selected = Idioma.elegido(context),
            onSelect = { codigo ->
                languageOpen = false
                // Si cambia el idioma en uso, la app se recrea con los textos nuevos.
                (context as? Activity)?.let { Idioma.cambiar(it, codigo) }
            },
            onDismiss = { languageOpen = false }
        )
    }
}

/** Lo que se muestra bajo "Idioma": el elegido o "Idioma del teléfono (Español)". */
@Composable
private fun languageSummary(context: Context): String =
    if (Idioma.elegido(context) == Idioma.SISTEMA)
        stringResource(R.string.settings_language_system_current, languageName(Idioma.actual(context)))
    else languageName(Idioma.actual(context))
