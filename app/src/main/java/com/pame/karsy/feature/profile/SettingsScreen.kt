package com.pame.karsy.feature.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid

private data class SettingsItem(val icon: String, val label: String, val desc: String)

private val settingsItems = listOf(
    SettingsItem("🔔", "Notificaciones", "Alertas y avisos de tu cuenta"),
    SettingsItem("🔒", "Privacidad y seguridad", "Contraseña, accesos y sesiones"),
    SettingsItem("🌐", "Idioma", "Español (México)"),
    SettingsItem("📄", "Términos y condiciones", "Políticas de privacidad y uso"),
)

/** Colores de la tarjeta de "Cerrar sesión" del mockup. */
private val LogoutBorder = Color(0xFFFECACA)
private val LogoutRed = Color(0xFFD93025)

/** Pantalla de configuración (WebConfigView del mockup). */
@Composable
fun SettingsScreen(onBack: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = "⚙️ Configuración",
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
                                // TODO: abrir la sección correspondiente (notificaciones, seguridad, idioma, términos)
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
                                item.label,
                                fontFamily = DmSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = KarsyCharcoal
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.desc,
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
                        .clickable(onClick = onLogout)
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("🚪", fontSize = 20.sp)
                    Text(
                        "Cerrar sesión",
                        fontFamily = DmSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = LogoutRed
                    )
                }
            }
        }
    }
}
