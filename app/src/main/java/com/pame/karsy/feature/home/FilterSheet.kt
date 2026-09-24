package com.pame.karsy.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.components.KarsyBrand
import com.pame.karsy.core.session.UserMode
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

internal val ORDEN_OPCIONES = listOf("Más recientes", "Menor precio", "Mayor precio")

/**
 * Panel lateral derecho con los filtros del marketplace (menú de filtros del mockup).
 * Las opciones salen de los catálogos de la BD. Los visitantes ven una invitación
 * a registrarse en lugar de los filtros.
 */
@Composable
fun FilterSheet(
    visible: Boolean,
    userMode: UserMode,
    filters: HomeFilters,
    options: FilterOptions,
    onApply: (HomeFilters) -> Unit,
    onDismiss: () -> Unit,
    onRegister: () -> Unit,
) {
    if (visible) BackHandler(onBack = onDismiss)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(KarsyNavy.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(220)) { it },
            exit = slideOutHorizontally(tween(180)) { it },
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            FilterPanel(
                userMode = userMode,
                filters = filters,
                options = options,
                onApply = onApply,
                onDismiss = onDismiss,
                onRegister = onRegister
            )
        }
    }
}

@Composable
private fun FilterPanel(
    userMode: UserMode,
    filters: HomeFilters,
    options: FilterOptions,
    onApply: (HomeFilters) -> Unit,
    onDismiss: () -> Unit,
    onRegister: () -> Unit,
) {
    // Borrador local: solo se aplica al tocar "Aplicar".
    var marca by rememberSaveable(filters) { mutableStateOf(filters.marca) }
    var modelo by rememberSaveable(filters) { mutableStateOf(filters.modelo) }
    var anio by rememberSaveable(filters) { mutableStateOf(filters.anio) }
    var tipo by rememberSaveable(filters) { mutableStateOf(filters.tipo) }
    var precioMin by rememberSaveable(filters) { mutableStateOf(filters.precioMin) }
    var precioMax by rememberSaveable(filters) { mutableStateOf(filters.precioMax) }
    var orden by rememberSaveable(filters) { mutableStateOf(filters.orden) }

    Column(
        Modifier
            .fillMaxHeight()
            .width(300.dp)
            .shadow(24.dp, RoundedCornerShape(0.dp), spotColor = KarsyNavy.copy(alpha = 0.3f))
            .background(KarsyWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            KarsyBrand(logoSize = 30.dp, fontSize = 17.sp)
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, contentDescription = "Cerrar", tint = KarsyMid, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = KarsyBorder)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            if (userMode.isLoggedIn) {
                HorizontalDivider(
                    color = KarsyBorder,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
                Text(
                    "FILTROS",
                    fontFamily = DmSans,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KarsyMid,
                    letterSpacing = 0.08.em,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 8.dp)
                )
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FilterField("Marca") {
                        KarsySelect(marca, options.marcas, { marca = it; modelo = TODOS })
                    }
                    FilterField("Modelo") { KarsySelect(modelo, options.modelos(marca), { modelo = it }) }
                    FilterField("Año") { KarsySelect(anio, options.anios, { anio = it }) }
                    FilterField("Tipo") { KarsySelect(tipo, options.tipos, { tipo = it }) }
                    FilterField("Rango de precio") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PriceInput(precioMin, { precioMin = it }, "Mín. $0", Modifier.weight(1f))
                            PriceInput(precioMax, { precioMax = it }, "Máx. $2M", Modifier.weight(1f))
                        }
                    }
                    FilterField("Ordenar por", bottom = 16) {
                        KarsySelect(orden, ORDEN_OPCIONES, { orden = it })
                    }
                    Row(
                        Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val limpio = HomeFilters()
                                marca = limpio.marca; modelo = limpio.modelo; anio = limpio.anio
                                tipo = limpio.tipo; precioMin = ""; precioMax = ""; orden = limpio.orden
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.5.dp, KarsyBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = KarsyWhite, contentColor = KarsyTextSecondary),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Limpiar", fontFamily = DmSans, fontSize = 13.sp)
                        }
                        Button(
                            onClick = {
                                onApply(HomeFilters(marca, modelo, anio, tipo, precioMin, precioMax, orden))
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                            contentPadding = PaddingValues(vertical = 10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aplicar", fontFamily = Outfit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                Column(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(KarsyTealLight)
                        .border(1.dp, KarsyTeal, RoundedCornerShape(14.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        "Crea una cuenta para acceder a los filtros y funciones exclusivas.",
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        lineHeight = 19.5.sp,
                        color = KarsyNavy,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                    Button(
                        onClick = onRegister,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
                        contentPadding = PaddingValues(vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Regístrate gratis →", fontFamily = Outfit, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterField(label: String, bottom: Int = 12, content: @Composable () -> Unit) {
    Column(Modifier.padding(bottom = bottom.dp)) {
        Text(
            label,
            fontFamily = DmSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = KarsyCharcoal,
            modifier = Modifier.padding(bottom = 5.dp)
        )
        content()
    }
}

@Composable
private fun PriceInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier) {
    val shape = RoundedCornerShape(10.dp)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = DmSans, fontSize = 12.sp, color = KarsyCharcoal),
        cursorBrush = SolidColor(KarsyTeal),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(KarsyBg)
                    .border(1.5.dp, KarsyBorder, shape)
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                if (value.isEmpty()) Text(placeholder, fontFamily = DmSans, fontSize = 12.sp, color = KarsyMid)
                inner()
            }
        }
    )
}

/**
 * Selector tipo <select> del mockup: caja con borde y menú desplegable.
 * También se usa para "Ordenar" en la pantalla principal.
 */
@Composable
internal fun KarsySelect(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    container: Color = KarsyBg,
    textColor: Color = KarsyCharcoal,
) {
    var expanded by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(modifier) {
        Row(
            Modifier
                .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .height(40.dp)
                .clip(shape)
                .background(container)
                .border(1.5.dp, KarsyBorder, shape)
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                selected,
                fontFamily = DmSans,
                fontSize = 13.sp,
                color = textColor,
                maxLines = 1,
                modifier = if (fillWidth) Modifier.weight(1f) else Modifier
            )
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = KarsyMid, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KarsyWhite
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opt,
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            color = if (opt == selected) KarsyTeal else KarsyCharcoal,
                            fontWeight = if (opt == selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
