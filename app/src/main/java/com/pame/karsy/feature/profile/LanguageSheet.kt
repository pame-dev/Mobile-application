package com.pame.karsy.feature.profile

import com.pame.karsy.core.theme.karsyTint
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.locale.Idioma
import com.pame.karsy.core.theme.Tema
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Nombre del idioma en su propio idioma ("Español" / "English"), como en cualquier selector. */
@Composable
internal fun languageName(code: String): String =
    stringResource(if (code == Idioma.EN) R.string.settings_language_en else R.string.settings_language_es)

/**
 * Selector de idioma de Configuración: español, inglés o seguir el idioma del teléfono.
 * [selected] es lo que eligió el usuario (Idioma.ES / EN / SISTEMA); al tocar una opción
 * se llama [onSelect] y la app se recrea en el idioma nuevo si cambia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSheet(selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KarsySurface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                stringResource(R.string.settings_language),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
            Text(
                stringResource(R.string.settings_language_sheet_desc),
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyMid,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            // Mismas banderas que el selector de la bienvenida.
            LanguageOption("🇪🇸", languageName(Idioma.ES), null, selected == Idioma.ES) { onSelect(Idioma.ES) }
            LanguageOption("🇬🇧", languageName(Idioma.EN), null, selected == Idioma.EN) { onSelect(Idioma.EN) }
            LanguageOption(
                flag = "📱",
                label = stringResource(R.string.settings_language_system),
                description = stringResource(R.string.settings_language_system_desc, languageName(Idioma.delTelefono())),
                selected = selected == Idioma.SISTEMA,
            ) { onSelect(Idioma.SISTEMA) }
        }
    }
}

@Composable
internal fun LanguageOption(flag: String, label: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) KarsyTealLight else KarsySurface)
            .border(1.5.dp, if (selected) KarsyTeal else KarsyBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(flag, fontSize = 22.sp)
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = DmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = KarsyCharcoal)
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(description, fontFamily = DmSans, fontSize = 12.sp, color = KarsyMid)
            }
        }
        // Botón de opción (radio)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) KarsyTeal else KarsyBorder, CircleShape)
        ) {
            if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(KarsyTeal))
        }
    }
}

/**
 * Selector de tema de Configuración: claro, oscuro o el del teléfono.
 * [selected] es Tema.CLARO / OSCURO / SISTEMA; la app se recrea si cambia el tema en uso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSheet(selected: String, phoneIsDark: Boolean, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KarsySurface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                stringResource(R.string.settings_theme),
                fontFamily = Outfit,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
            Text(
                stringResource(R.string.settings_theme_sheet_desc),
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = KarsyMid,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ThemeOption(Icons.Outlined.LightMode, stringResource(R.string.settings_theme_light), null, selected == Tema.CLARO) { onSelect(Tema.CLARO) }
            ThemeOption(Icons.Outlined.DarkMode, stringResource(R.string.settings_theme_dark), null, selected == Tema.OSCURO) { onSelect(Tema.OSCURO) }
            ThemeOption(
                icon = Icons.Outlined.Smartphone,
                label = stringResource(R.string.settings_theme_system),
                description = stringResource(
                    R.string.settings_language_system_desc,
                    stringResource(if (phoneIsDark) R.string.settings_theme_dark else R.string.settings_theme_light)
                ),
                selected = selected == Tema.SISTEMA,
            ) { onSelect(Tema.SISTEMA) }
        }
    }
}

/**
 * Opción del selector de tema con ícono vectorial (sol, luna, teléfono).
 * Activa: fondo azul hielo, borde turquesa de 1.5 dp y radio relleno.
 * Inactiva: fondo de tarjeta, borde fino de 1 dp y radio vacío.
 */
@Composable
private fun ThemeOption(icon: ImageVector, label: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) karsyTint(KarsyTeal, Color(0xFFE2F1F8)) else KarsySurface)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) KarsyTeal else ThemeOptionBorder, shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) KarsyTeal else KarsyMid, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontFamily = DmSans, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = KarsyCharcoal)
            if (description != null) {
                Spacer(Modifier.height(2.dp))
                Text(description, fontFamily = DmSans, fontSize = 12.sp, color = KarsyMid)
            }
        }
        // Botón de opción (radio)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) KarsyTeal else KarsyBorder, CircleShape)
        ) {
            if (selected) Box(Modifier.size(11.dp).clip(CircleShape).background(KarsyTeal))
        }
    }
}

/** Borde fino de las opciones de tema no elegidas (#E5E9EC en claro). */
private val ThemeOptionBorder: Color get() = if (Tema.oscuro) KarsyBorder else Color(0xFFE5E9EC)
