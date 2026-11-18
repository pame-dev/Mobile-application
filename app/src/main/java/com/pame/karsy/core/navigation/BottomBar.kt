package com.pame.karsy.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyCoral
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavyDeep
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Tema

/** Pestañas de la barra inferior (el "+" central no es pestaña: abre Publicar). */
enum class BottomTab { Home, Lots, Favorites, Notifications }

/**
 * Menú de la cuenta para el botón de Perfil de los encabezados. El estado (abierto, puntito de
 * pendientes) y el menú en sí los maneja NavGraph; cada encabezado solo dibuja su botón con su
 * estilo y llama a [menu] dentro del mismo Box, así el menú se ancla al botón y abre hacia abajo.
 */
class MenuCuenta(
    val abierto: Boolean,
    /** Admin o moderador con pendientes en el panel. */
    val puntito: Boolean,
    val alternar: () -> Unit,
    val menu: @Composable () -> Unit,
)

/** null fuera de las pantallas de NavGraph (p. ej. en vistas previas). */
val LocalMenuCuenta = compositionLocalOf<MenuCuenta?> { null }

/** Puntito de pendientes en la esquina superior derecha del botón de Perfil del encabezado. */
@Composable
fun BoxScope.PuntitoPerfil() {
    Box(
        Modifier
            .align(Alignment.TopEnd)
            .offset(x = 3.dp, y = (-3).dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(KarsyCoral)
            .border(1.5.dp, KarsySurface, CircleShape)
    )
}

/**
 * Espacio que ocupa la barra inferior flotante sobre el contenido (0 cuando no se muestra).
 * Las pantallas con barra lo suman al padding inferior de su lista para que lo último
 * no quede debajo de ella. No incluye la barra de navegación del sistema.
 */
val LocalBottomBarSpace = compositionLocalOf { 0.dp }

/** Alto de la barra (64) + margen inferior (4) + aire sobre la barra. */
internal val BOTTOM_BAR_SPACE: Dp = 80.dp

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
 * Barra inferior flotante: Inicio, Lotes, "+" (Publicar), Favoritos y Notificaciones, solo íconos.
 * El Perfil (y su menú) está en el encabezado de cada pantalla (ver [MenuCuenta]).
 */
@Composable
fun KarsyBottomBar(
    current: BottomTab?,
    onTab: (BottomTab) -> Unit,
    onPublish: () -> Unit,
    /** Avisos sin leer: globito sobre la campana (sin globito si es 0). */
    unread: Int,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(KarsyRadius.Xxl)
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            // Pegada al borde: solo 4 dp sobre la barra de navegación del sistema.
            .padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
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
            // "+" central, dentro de la barra y en la misma línea que los demás íconos:
            // 56 dp en una barra de 64 dp (4 dp libres arriba y abajo).
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(KarsyNavyDeep)
                        .border(2.dp, KarsySurface, CircleShape)
                        .clickable(onClick = onPublish),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.home_fab_add_vehicle), tint = KarsyWhite, modifier = Modifier.size(28.dp))
                }
            }
            TabItem(BottomTab.Favorites, current, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, stringResource(R.string.nav_favorites), onTab)
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val active = current == BottomTab.Notifications
                Pill(
                    active = active,
                    icon = if (active) Icons.Rounded.Notifications else Icons.Outlined.Notifications,
                    description = if (unread > 0) pluralStringResource(R.plurals.admin2_notifications_unread, unread, unread)
                    else stringResource(R.string.home_notifications),
                    onClick = { onTab(BottomTab.Notifications) },
                    badge = unread
                )
            }
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
        Pill(active, if (active) activeIcon else inactiveIcon, description, onClick = { onTab(tab) })
    }
}

/** Ícono de pestaña; activo va dentro de una cápsula azul hielo. */
@Composable
private fun Pill(
    active: Boolean,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    /** Avisos sin leer: globito rojo con el número, en la esquina del ícono. */
    badge: Int = 0,
) {
    // El globito va en un Box sin recorte encima de la cápsula, para que la forma redonda no lo corte.
    Box(Modifier.width(52.dp).height(36.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
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
        Box(Modifier.size(23.dp)) {
            // Mismo rojo y conteo que la campana de antes, a la medida del ícono (no se sale de la barra).
            if (badge > 0) Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-6).dp)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                    .clip(CircleShape)
                    .background(KarsyError)
                    .border(1.5.dp, KarsySurface, CircleShape)
                    .padding(horizontal = 3.dp)
            ) {
                Text(
                    if (badge > 99) "99+" else badge.toString(),
                    fontFamily = DmSans,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyWhite
                )
            }
        }
    }
}
