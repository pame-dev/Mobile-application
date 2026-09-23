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
fun AdminVehiclesSection() {
    var search by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminVehicle?>(null) }

    // TODO: cargar publicaciones de vehículos desde Supabase
    val filtered = AdminSampleData.vehicles.filter { vehicle ->
        val matchesSearch = "${vehicle.name} ${vehicle.seller}".contains(search, ignoreCase = true)
        val matchesStatus = status == "Todos" || vehicle.status == status
        matchesSearch && matchesStatus
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = "Vehículos",
            description = "Revisa las publicaciones de vehículos y su estado dentro de la plataforma."
        )
        AdminCard {
            AdminFilterBar {
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = "Buscar marca, modelo o vendedor...",
                    modifier = Modifier.fillMaxWidth()
                )
                AdminSelect(
                    value = status,
                    options = listOf("Todos", "Activo", "Pendiente", "Deshabilitado"),
                    onSelect = { status = it }
                )
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No se encontraron vehículos",
                    description = "Prueba con otra marca, modelo, vendedor o estado."
                )
            } else {
                filtered.forEachIndexed { index, vehicle ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                vehicle.name,
                                fontFamily = DmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy,
                                modifier = Modifier.weight(1f)
                            )
                            AdminBadge(vehicle.status, vehicleStatusTone(vehicle.status))
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField("Vendedor", vehicle.seller, Modifier.weight(1.6f))
                            AdminField("Año", vehicle.year.toString(), Modifier.weight(0.7f))
                            AdminField("Precio", vehicle.price, Modifier.weight(1f), bold = true)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AdminActionButton("Revisar", onClick = { selected = vehicle })
                        }
                    }
                }
            }
        }
    }

    selected?.let { vehicle ->
        // TODO: aprobar/rechazar propuestas_publicacion o deshabilitar la publicación
        //  + insertar en acciones_administrativas
        AdminDetailModal(
            title = "Detalle del vehículo",
            subtitle = vehicle.name,
            onDismiss = { selected = null }
        ) {
            DetailRow("Vehículo", vehicle.name)
            DetailRow("Transmisión", "Automática")
            DetailRow("Kilometraje", "45,000 km")
            DetailRow("Carrocería", "Sedán")
            DetailRow("Color", "Blanco perla")
            DetailRow("Estado", vehicle.status)
        }
    }
}

private fun vehicleStatusTone(status: String) = when (status) {
    "Activo" -> BadgeTone.Success
    "Pendiente" -> BadgeTone.Warning
    else -> BadgeTone.Danger
}
