package com.pame.karsy.core.components

import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.util.ArchivoConstancia
import com.pame.karsy.core.util.Constancia

/**
 * Campo para subir la Constancia de Situación Fiscal (registro del lote y perfil).
 * Abre el selector de documentos con PDF e imágenes; [onPick] recibe el archivo y quien
 * lo use lo revisa con Constancia.revisar. Con [archivo] muestra su nombre, tamaño y
 * los botones para cambiarlo o quitarlo.
 */
@Composable
fun ConstanciaField(
    archivo: ArchivoConstancia?,
    error: String?,
    onPick: (Uri) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.form_constancia_label),
    showHelp: Boolean = true,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onPick(uri)
    }
    val abrir = { picker.launch(Constancia.TIPOS_SELECTOR) }
    val shape = RoundedCornerShape(12.dp)

    Column(modifier.padding(bottom = 16.dp)) {
        Text(
            label,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if (showHelp) Text(
            stringResource(R.string.form_constancia_help),
            fontFamily = DmSans,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            color = KarsyMid,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .alpha(if (enabled) 1f else 0.6f)
                .clip(shape)
                .background(KarsySurface)
                .border(1.dp, if (error != null) KarsyError else KarsyBorder, shape)
                .then(if (archivo == null) Modifier.clickable(enabled = enabled, role = Role.Button, onClick = abrir) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KarsyTealLight)
            ) {
                Icon(
                    Icons.Outlined.Description,
                    contentDescription = null,
                    tint = KarsyTeal,
                    modifier = Modifier.size(20.dp)
                )
            }
            if (archivo == null) {
                Text(
                    stringResource(R.string.form_constancia_pick),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = KarsyTeal,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Column(Modifier.weight(1f)) {
                    Text(
                        archivo.nombre,
                        fontFamily = DmSans,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyCharcoal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        Formatter.formatShortFileSize(context, archivo.tamano),
                        fontFamily = DmSans,
                        fontSize = 12.sp,
                        color = KarsyMid
                    )
                }
                FieldAction(stringResource(R.string.form_constancia_change), enabled, abrir)
                FieldAction(stringResource(R.string.form_constancia_remove), enabled, onRemove)
            }
        }

        if (error != null) {
            Text(
                error,
                fontFamily = DmSans,
                fontSize = 12.sp,
                color = KarsyError,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun FieldAction(text: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text,
        fontFamily = DmSans,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = KarsyTeal,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    )
}
