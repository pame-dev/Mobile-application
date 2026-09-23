package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy

@Composable
fun AdminLotesSection() {
    var search by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminLot?>(null) }

    // TODO: cargar lotes/agencias registrados desde Supabase
    val filtered = AdminSampleData.lots.filter { lot ->
        "${lot.name} ${lot.responsible} ${lot.city}".contains(search, ignoreCase = true)
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = "Lotes",
            description = "Administra los lotes y agencias que publican vehículos en Karsy."
        )
        AdminCard {
            AdminFilterBar {
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Buscar lote, responsable o ciudad...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No se encontraron lotes",
                    description = "Prueba con otro nombre, responsable o ciudad."
                )
            } else {
                filtered.forEachIndexed { index, lot ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                lot.name,
                                fontFamily = DmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy,
                                modifier = Modifier.weight(1f)
                            )
                            AdminBadge(lot.status, if (lot.status == "Activo") BadgeTone.Success else BadgeTone.Warning)
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField("Responsable", lot.responsible, Modifier.weight(1.3f))
                            AdminField("Ubicación", lot.city, Modifier.weight(1.2f))
                            AdminField("Vehículos", lot.vehicles.toString(), Modifier.weight(0.8f), bold = true)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AdminActionButton("Ver lote", onClick = { selected = lot })
                        }
                    }
                }
            }
        }
    }

    selected?.let { lot ->
        // TODO: aprobar / suspender lote + insertar en acciones_administrativas
        AdminDetailModal(
            title = "Detalle del lote",
            subtitle = lot.name,
            onDismiss = { selected = null }
        ) {
            DetailRow("Nombre del lote", lot.name)
            DetailRow("Responsable", lot.responsible)
            DetailRow("Ciudad", lot.city)
            DetailRow("Vehículos publicados", lot.vehicles.toString())
            DetailRow("Estado", lot.status)
        }
    }
}
