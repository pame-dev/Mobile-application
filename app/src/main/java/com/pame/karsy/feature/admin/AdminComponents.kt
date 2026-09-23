package com.pame.karsy.feature.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorder
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTextSecondary
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit

/** Colores propios del panel de administración (tomados del mockup). */
internal object AdminColors {
    val Blue = Color(0xFF2563EB)
    val TableText = Color(0xFF344054)
    val Muted = Color(0xFF98A2B3)
    val RowDivider = Color(0xFFF0F2F4)
    val Danger = Color(0xFFE65B5B)
    val BadgeRed = Color(0xFFEF4444)
    val Green = Color(0xFF22C55E)
    val NeutralText = Color(0xFF475467)
}

enum class BadgeTone(val bg: Color, val fg: Color) {
    Success(Color(0xFFECFDF3), Color(0xFF16824A)),
    Warning(Color(0xFFFFFAEB), Color(0xFFB54708)),
    Danger(Color(0xFFFEF3F2), Color(0xFFB42318)),
    Info(Color(0xFFEFF8FF), Color(0xFF175CD3)),
    Neutral(Color(0xFFF2F4F7), Color(0xFF475467)),
}

enum class ActionTone(val bg: Color, val fg: Color, val border: Color) {
    Default(Color(0xFFF4F7F9), Color(0xFF0D2B45), Color(0xFFE2E8ED)),
    Danger(Color(0xFFFEF3F2), Color(0xFFB42318), Color(0xFFFECACA)),
    Success(Color(0xFFECFDF3), Color(0xFF16824A), Color(0xFFABEFC6)),
}

@Composable
fun AdminBadge(text: String, tone: BadgeTone = BadgeTone.Neutral, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = tone.fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(tone.bg)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    )
}

@Composable
fun AdminSectionHeader(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Text(
            text = title,
            fontFamily = Outfit,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyNavy,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            fontFamily = DmSans,
            fontSize = 14.sp,
            color = KarsyMid,
            lineHeight = 20.sp,
        )
    }
}

@Composable
fun AdminSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(fontFamily = DmSans, fontSize = 13.sp, color = KarsyCharcoal),
        cursorBrush = SolidColor(KarsyNavy),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KarsyWhite)
                    .border(1.dp, KarsyBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 13.dp)
            ) {
                Icon(
                    Icons.Rounded.Search,
                    contentDescription = null,
                    tint = AdminColors.Muted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            placeholder,
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            color = AdminColors.Muted,
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        }
    )
}

/** Equivalente al <select> del mockup. */
@Composable
fun AdminSelect(
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(KarsyWhite)
                .border(1.dp, KarsyBorder, RoundedCornerShape(10.dp))
                .clickable { expanded = true }
                .padding(start = 12.dp, end = 8.dp)
        ) {
            Text(value, fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.TableText)
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = KarsyTextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KarsyWhite,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            option,
                            fontFamily = DmSans,
                            fontSize = 13.sp,
                            fontWeight = if (option == value) FontWeight.Bold else FontWeight.Normal,
                            color = if (option == value) KarsyNavy else AdminColors.TableText
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                1.dp, shape,
                ambientColor = KarsyNavy.copy(alpha = 0.08f),
                spotColor = KarsyNavy.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(KarsyWhite)
            .border(1.dp, KarsyBorder, shape),
        content = content
    )
}

@Composable
fun AdminActionButton(
    text: String,
    modifier: Modifier = Modifier,
    tone: ActionTone = ActionTone.Default,
    onClick: () -> Unit = {},
) {
    Text(
        text = text,
        fontFamily = DmSans,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = tone.fg,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tone.bg)
            .border(1.dp, tone.border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}

@Composable
fun AdminEmptyState(icon: ImageVector, title: String, description: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 58.dp)
    ) {
        Icon(icon, contentDescription = null, tint = KarsyTextSecondary, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            fontFamily = Outfit,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = KarsyNavy,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            fontFamily = DmSans,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            color = KarsyTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Fila de "tabla" adaptada a teléfono: bloque con divisor inferior. */
@Composable
fun AdminListRow(isLast: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(14.dp),
        content = content
    )
    if (!isLast) HorizontalDivider(thickness = 1.dp, color = AdminColors.RowDivider)
}

/** Campo con el nombre de la columna de la tabla original como etiqueta. */
@Composable
fun AdminField(label: String, value: String, modifier: Modifier = Modifier, bold: Boolean = false) {
    Column(modifier) {
        Text(
            label.uppercase(),
            fontFamily = DmSans,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.04.em,
            color = KarsyTextSecondary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = AdminColors.TableText
        )
    }
}

/** Barra superior de filtros dentro de una AdminCard. */
@Composable
fun AdminFilterBar(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        content = content
    )
    HorizontalDivider(thickness = 1.dp, color = KarsyBorder)
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp)
    ) {
        Text(
            label,
            fontFamily = DmSans,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = AdminColors.Muted,
            modifier = Modifier.width(120.dp)
        )
        Text(
            value,
            fontFamily = DmSans,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = AdminColors.TableText,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(thickness = 1.dp, color = AdminColors.RowDivider)
}

@Composable
fun AdminDetailModal(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(KarsyWhite)
                .verticalScroll(rememberScrollState())
                .padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(title, fontFamily = Outfit, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KarsyNavy)
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, fontFamily = DmSans, fontSize = 12.sp, color = AdminColors.Muted)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(KarsyBg)
                        .border(1.dp, KarsyBorder, RoundedCornerShape(9.dp))
                        .clickable(onClick = onDismiss)
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cerrar",
                        tint = AdminColors.NeutralText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            content()
        }
    }
}

/** Contenedor desplazable común de cada sección. */
@Composable
fun AdminSectionScroll(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        content = content
    )
}
