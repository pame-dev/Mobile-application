package com.pame.karsy.feature.profile

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import com.pame.karsy.R
import com.pame.karsy.core.theme.KarsyTeal

/**
 * Portada de un perfil: la foto de portada si hay, o el degradado de la marca.
 * Lleva una sombra suave arriba para que los íconos encima se lean. No se amplía al tocarla.
 */
@Composable
internal fun ProfileCover(coverUrl: String?, height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(ProfileCoverBrush)
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Transparent)))
            )
        }
    }
}

/**
 * Avatar que se amplía al tocarlo (solo si hay foto; con iniciales no hace nada).
 * Mismos parámetros que [ProfileAvatar].
 */
@Composable
internal fun ZoomableProfileAvatar(
    name: String,
    avatarUrl: String?,
    size: Dp,
    initialsSize: TextUnit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileAvatar(
        name = name,
        avatarUrl = avatarUrl,
        size = size,
        initialsSize = initialsSize,
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = avatarUrl != null, onClickLabel = stringResource(R.string.profile_view_photo_cd), onClick = onOpen)
    )
}

/**
 * Foto de perfil ampliada (estilo vista previa de avatar): fondo oscuro difuminado,
 * la foto grande y circular con borde, ✕ arriba a la derecha. Tocar el fondo también cierra.
 * El difuminado del fondo necesita Android 12+; en versiones anteriores solo se oscurece.
 */
@Composable
internal fun AvatarViewer(name: String, avatarUrl: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val blurPx = with(LocalDensity.current) { 12.dp.roundToPx() }
        SideEffect {
            window?.setDimAmount(0f)
            if (window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply { blurBehindRadius = blurPx }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClose
                ),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(32.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .border(3.dp, KarsyTeal.copy(alpha = 0.9f), CircleShape)
                    // Tocar la foto no cierra.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.profile_close_cd), tint = Color.White)
            }
        }
    }
}
