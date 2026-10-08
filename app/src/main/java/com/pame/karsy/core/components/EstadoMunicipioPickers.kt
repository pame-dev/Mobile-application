package com.pame.karsy.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.location.MexicoCatalogo
import com.pame.karsy.core.location.Lugares
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.Outfit

/** Tamaños de los campos: como FormInput (registro) o como los de Editar perfil. */
enum class PickerStyle(val labelSize: TextUnit, val corner: Dp, val minHeight: Dp, val bottom: Dp) {
    Form(13.sp, 12.dp, 50.dp, 16.dp),
    Compact(12.sp, 10.dp, 48.dp, 0.dp),
}

/**
 * Estado y municipio de México como desplegables con buscador. El municipio depende del
 * estado: se habilita al elegirlo y se borra si se cambia de estado.
 */
@Composable
fun EstadoMunicipioPickers(
    estado: String,
    municipio: String,
    onEstado: (String) -> Unit,
    onMunicipio: (String) -> Unit,
    modifier: Modifier = Modifier,
    estadoLabel: String = stringResource(R.string.auth_state),
    municipioLabel: String = stringResource(R.string.auth_city),
    estadoError: String? = null,
    municipioError: String? = null,
    style: PickerStyle = PickerStyle.Form,
) {
    val context = LocalContext.current
    val estados = remember { MexicoCatalogo.estados(context) }
    val municipios = remember(estado) { MexicoCatalogo.municipios(context, estado) }
    var abierto by rememberSaveable { mutableStateOf<String?>(null) } // "estado" | "municipio"

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier.fillMaxWidth()) {
        PickerField(
            label = estadoLabel,
            value = estado,
            placeholder = stringResource(R.string.location_pick_state),
            error = estadoError,
            style = style,
            modifier = Modifier.weight(1f),
            onClick = { abierto = "estado" }
        )
        PickerField(
            label = municipioLabel,
            value = municipio,
            placeholder = stringResource(
                if (municipios.isEmpty()) R.string.location_pick_state_first else R.string.location_pick_city
            ),
            error = municipioError,
            enabled = municipios.isNotEmpty(),
            style = style,
            modifier = Modifier.weight(1f),
            onClick = { abierto = "municipio" }
        )
    }

    when (abierto) {
        "estado" -> SearchPickerSheet(
            title = estadoLabel,
            options = estados,
            selected = MexicoCatalogo.estadoDelCatalogo(context, estado),
            onSelect = { nuevo ->
                abierto = null
                onEstado(nuevo)
                // Un municipio de otro estado no sirve: se borra para que lo elijan de nuevo.
                if (MexicoCatalogo.municipios(context, nuevo).none { Lugares.mismoLugar(it, municipio) }) {
                    onMunicipio("")
                }
            },
            onDismiss = { abierto = null }
        )
        "municipio" -> SearchPickerSheet(
            title = municipioLabel,
            options = municipios,
            selected = municipios.firstOrNull { Lugares.mismoLugar(it, municipio) },
            onSelect = { abierto = null; onMunicipio(it) },
            onDismiss = { abierto = null }
        )
    }
}

@Composable
private fun PickerField(
    label: String,
    value: String,
    placeholder: String,
    error: String?,
    style: PickerStyle,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(style.corner)
    Column(modifier.padding(bottom = style.bottom)) {
        Text(
            label,
            fontFamily = DmSans,
            fontSize = style.labelSize,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = style.minHeight)
                .clip(shape)
                .background(if (enabled) KarsySurface else KarsyBg)
                .border(1.dp, if (error != null) KarsyError else KarsyBorder, shape)
                .clickable(enabled = enabled, role = Role.DropdownList, onClick = onClick)
                .padding(start = 14.dp, end = 8.dp)
        ) {
            Text(
                value.ifBlank { placeholder },
                fontFamily = DmSans,
                fontSize = 15.sp,
                color = if (value.isBlank()) KarsyMid else KarsyCharcoal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(22.dp))
        }
        if (error != null) {
            Text(error, fontFamily = DmSans, fontSize = 12.sp, color = KarsyError, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
        }
    }
}

/** Hoja con buscador (ignora acentos) para elegir de una lista larga. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchPickerSheet(
    title: String,
    options: List<String>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = remember(query, options) {
        if (query.isBlank()) options else options.filter { contiene(it, query) }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = KarsySurface,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).imePadding()) {
            Text(title, fontFamily = Outfit, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
            Spacer(Modifier.size(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.location_search), fontFamily = DmSans, fontSize = 15.sp, color = KarsyMid) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = KarsyMid) },
                textStyle = TextStyle(fontFamily = DmSans, fontSize = 15.sp, color = KarsyCharcoal),
                shape = RoundedCornerShape(12.dp),
                colors = karsyTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.size(8.dp))
            if (filtered.isEmpty()) {
                Text(
                    stringResource(R.string.location_no_results),
                    fontFamily = DmSans,
                    fontSize = 14.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp).navigationBarsPadding()) {
                items(filtered, key = { it }) { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.RadioButton) { onSelect(option) }
                            .padding(vertical = 14.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            option,
                            fontFamily = DmSans,
                            fontSize = 15.sp,
                            fontWeight = if (option == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == selected) KarsyTeal else KarsyCharcoal,
                            modifier = Modifier.weight(1f)
                        )
                        if (option == selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(thickness = 1.dp, color = KarsyBorder)
                }
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}

/** "torreon" encuentra "Torreón": sin acentos ni mayúsculas. */
private fun contiene(texto: String, buscado: String): Boolean {
    fun n(s: String) = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "").lowercase()
    return n(texto).contains(n(buscado.trim()))
}
