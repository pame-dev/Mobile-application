package com.pame.karsy.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavyDeep
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Tema

/** Pestañas de la barra inferior (el "+" central no es pestaña: abre Publicar). */
enum class BottomTab { Home, Lots, Favorites, Profile }

/**
 * Espacio que ocupa la barra inferior flotante sobre el contenido (0 cuando no se muestra).
 * Las pantallas con barra lo suman al padding inferior de su lista para que lo último
 * no quede debajo de ella. No incluye la barra de navegación del sistema.
 */
val LocalBottomBarSpace = compositionLocalOf { 0.dp }

/** Alto de la barra + margen inferior + lo que sobresale el "+". */
internal val BOTTOM_BAR_SPACE: Dp = 92.dp

/**
 * Paneles que tapan la pantalla (filtros, menú del admin, modales propios) ocultan la
 * barra mientras están abiertos. Es un contador para que varios puedan pedirlo a la vez.
 */
internal object BottomBarOverlays {
    var count by mutableIntStateOf(0)
}

/** Oculta la barra inferior mientras [hidden] sea true (y mientras la pantalla exista). */
@Composable
fun HideBottomBarWhile(hidden: Boolean) {
    DisposableEffect(hidden) {
        if (hidden) BottomBarOverlays.count++
        onDispose { if (hidden) BottomBarOverlays.count-- }
    }
}

// Colores del diseño; en tema oscuro se usan equivalentes que mantienen el contraste.
private val BarOutline: Color get() = if (Tema.oscuro) Color(0xFF2C3844) else Color(0xFFD1E1E5)
private val ActivePill: Color get() = if (Tema.oscuro) Color(0xFF1F3A4F) else Color(0xFFE2F1F8)
private val ActiveIcon: Color get() = if (Tema.oscuro) KarsyInk else KarsyNavyDeep
private val BarShadow = KarsyNavyDeep.copy(alpha = 0.10f)

/**
 * Barra inferior flotante: Inicio, Lotes, "+" (Publicar), Favoritos y Perfil, solo íconos.
 * [profileMenu] se dibuja anclado al ícono de Perfil (el menú de la cuenta se abre hacia arriba).
 */
@Composable
fun KarsyBottomBar(
    current: BottomTab?,
    onTab: (BottomTab) -> Unit,
    onPublish: () -> Unit,
    profileMenuOpen: Boolean,
    profileMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(14.dp, shape, ambientColor = BarShadow, spotColor = BarShadow)
                .clip(shape)
                .background(KarsySurface)
                .border(1.dp, BarOutline, shape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem(BottomTab.Home, current, Icons.Rounded.Home, Icons.Outlined.Home, stringResource(R.string.nav_home), onTab)
            TabItem(BottomTab.Lots, current, Icons.Rounded.Business, Icons.Outlined.Business, stringResource(R.string.nav_lots), onTab)
            // Hueco para el "+" que sobresale.
            Box(Modifier.weight(1f))
            TabItem(BottomTab.Favorites, current, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, stringResource(R.string.nav_favorites), onTab)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Pill(
                    active = current == BottomTab.Profile || profileMenuOpen,
                    icon = if (current == BottomTab.Profile || profileMenuOpen) Icons.Rounded.Person else Icons.Outlined.Person,
                    description = stringResource(R.string.nav_profile),
                    onClick = { onTab(BottomTab.Profile) }
                )
                // El menú se ancla a este Box: como no cabe abajo, se abre hacia arriba.
                Box(Modifier.align(Alignment.TopEnd)) { profileMenu() }
            }
        }

        // "+" central: sobresale por encima de la barra.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp)
                .size(56.dp)
                .shadow(8.dp, CircleShape, ambientColor = BarShadow, spotColor = KarsyNavyDeep.copy(alpha = 0.25f))
                .clip(CircleShape)
                .background(KarsyNavyDeep)
                .border(3.dp, KarsySurface, CircleShape)
                .clickable(onClick = onPublish),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_fab_add_vehicle), tint = KarsyWhite, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.TabItem(
    tab: BottomTab,
    current: BottomTab?,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    description: String,
    onTab: (BottomTab) -> Unit,
) {
    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
        val active = tab == current
        Pill(active, if (active) activeIcon else inactiveIcon, description) { onTab(tab) }
    }
}

/** Ícono de pestaña; activo va dentro de una cápsula azul hielo. */
@Composable
private fun Pill(active: Boolean, icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        Modifier
            .width(52.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(if (active) ActivePill else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (active) ActiveIcon else KarsyMid, modifier = Modifier.size(23.dp))
    }
}
