package com.pame.karsy.feature.publish.steps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.feature.publish.FieldLabel
import com.pame.karsy.feature.publish.PublishStepScaffold
import com.pame.karsy.feature.publish.PublishTextInput
import com.pame.karsy.feature.publish.PublishViewModel
import com.pame.karsy.feature.publish.SectionCard
import com.pame.karsy.feature.publish.SectionTitle

// TODO: cargar catálogos marcas/modelos/colores/transmisiones/carrocerias desde Supabase
private val marcas = listOf(
    "BMW", "Mercedes-Benz", "Audi", "Toyota", "Honda", "Nissan", "Volkswagen", "Ford",
    "Chevrolet", "Mazda", "Kia", "Hyundai", "Dodge", "Jeep", "Subaru", "Volvo",
    "Porsche", "Lexus", "MINI", "Otro",
)
private val transmisiones = listOf("Manual", "Automática")
private val tiposCarro = listOf("Sedán", "SUV", "Deportivo", "Pickup", "Convertible", "Hatchback", "Van")
private val carColors = listOf(
    "Blanco" to Color(0xFFFFFFFF),
    "Negro" to Color(0xFF1A1A1A),
    "Plata" to Color(0xFFB0B8C1),
    "Gris" to Color(0xFF6B7280),
    "Azul" to Color(0xFF2563EB),
    "Marino" to Color(0xFF0D2B45),
    "Verde" to Color(0xFF16A34A),
    "Dorado" to Color(0xFFD97706),
    "Beige" to Color(0xFFC9A87C),
    "Naranja" to Color(0xFFEA580C),
)

/** Paso 1: datos generales del vehículo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DatosStep(form: PublishViewModel, onNext: () -> Unit, onBack: () -> Unit) {
    PublishStepScaffold(
        step = 1,
        onBack = onBack,
        buttonText = "Siguiente: Fotos →",
        onButtonClick = onNext
    ) {
        SectionCard {
            SectionTitle("INFORMACIÓN DEL VEHÍCULO")

            Column {
                FieldLabel("MARCA")
                MarcaDropdown(value = form.marca, onSelect = { form.marca = it })
            }

            // TODO: cambiar a selector de modelos filtrado por marca cuando exista el catálogo
            Column {
                FieldLabel("MODELO")
                PublishTextInput(form.modelo, { form.modelo = it }, "Ej. Serie 3 320i")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("AÑO")
                    PublishTextInput(form.anio, { form.anio = it }, "2023", numeric = true)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("PRECIO ($)")
                    PublishTextInput(form.precio, { form.precio = it }, "685,000", numeric = true)
                }
            }

            Column {
                FieldLabel("TRANSMISIÓN")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    transmisiones.forEach { t ->
                        val selected = form.transmision == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) KarsyNavy else KarsyBg)
                                .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(12.dp))
                                .clickable { form.transmision = t }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t,
                                fontFamily = DmSans,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) KarsyWhite else KarsyMid
                            )
                        }
                    }
                }
            }

            Column {
                FieldLabel("KILOMETRAJE")
                PublishTextInput(form.kilometraje, { form.kilometraje = it }, "18,500 km", numeric = true)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    FieldLabel("CILINDROS")
                    PublishTextInput(form.cilindros, { form.cilindros = it }, "4", numeric = true)
                }
                Column(Modifier.weight(1f)) {
                    FieldLabel("CABALLOS DE FUERZA")
                    PublishTextInput(form.caballos, { form.caballos = it }, "184 hp", numeric = true)
                }
            }
        }

        SectionCard {
            SectionTitle("CLASIFICACIÓN")

            Column {
                FieldLabel("TIPO DE CARRO")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tiposCarro.forEach { t ->
                        val selected = form.tipo == t
                        Text(
                            text = t,
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) KarsyWhite else KarsyMid,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) KarsyNavy else KarsyBg)
                                .border(1.5.dp, if (selected) KarsyNavy else KarsyBorderMuted, RoundedCornerShape(50))
                                .clickable { form.tipo = t }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Column {
                FieldLabel("COLOR")
                FlowRow(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    carColors.forEach { (name, hex) ->
                        ColorSwatch(
                            name = name,
                            color = hex,
                            selected = form.color == name,
                            onClick = { form.color = name }
                        )
                    }
                }
            }
        }

        SectionCard {
            SectionTitle("INFORMACIÓN ADICIONAL")
            Column {
                FieldLabel("CANTIDAD DE DUEÑOS")
                PublishTextInput(form.duenos, { form.duenos = it }, "1", numeric = true)
            }
        }
    }
}

@Composable
private fun MarcaDropdown(value: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(KarsyBg)
                .border(1.5.dp, KarsyBorderMuted, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value.ifEmpty { "Selecciona una marca" },
                fontFamily = DmSans,
                fontSize = 14.sp,
                color = if (value.isNotEmpty()) KarsyCharcoal else KarsyMid,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = KarsyMid,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KarsyWhite,
            modifier = Modifier.heightIn(max = 320.dp)
        ) {
            marcas.forEach { m ->
                DropdownMenuItem(
                    text = {
                        Text(m, fontFamily = DmSans, fontSize = 14.sp, color = KarsyCharcoal)
                    },
                    onClick = {
                        onSelect(m)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .then(if (selected) Modifier.border(2.dp, KarsyTeal, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        if (selected) 3.dp else 2.dp,
                        if (selected) KarsyNavy else KarsyBorderMuted,
                        CircleShape
                    )
            )
        }
        Text(
            text = name,
            fontFamily = DmSans,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) KarsyNavy else KarsyMid,
            textAlign = TextAlign.Center
        )
    }
}
