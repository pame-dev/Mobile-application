package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy

@Composable
fun AdminVehiclesSection(vm: AdminViewModel, onCarClick: (Long) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminVehicle?>(null) }
    // Publicación a la que se le está escribiendo el motivo de deshabilitación.
    var disabling by remember { mutableStateOf<AdminVehicle?>(null) }

    val filtered = vm.vehicles.filter { vehicle ->
        val matchesSearch = "${vehicle.name} ${vehicle.seller}".contains(search, ignoreCase = true)
        val matchesStatus = status == "Todos" || vehicle.status == status
        matchesSearch && matchesStatus
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_section_vehicles),
            description = stringResource(R.string.admin2_vehicles_desc)
        )
        AdminCard {
            AdminFilterBar {
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = stringResource(R.string.admin2_vehicles_search),
                    modifier = Modifier.fillMaxWidth()
                )
                // Los estados se comparan en español; solo se traduce lo que se muestra.
                val statusValues = listOf(
                    "Todos", "Activo", "Pendiente", "Cambios pendientes", "Rechazado",
                    "Deshabilitado", "Pausado", "Vendido"
                )
                val statusLabels = statusValues.map { adminValueLabel(it) }
                AdminSelect(
                    value = adminValueLabel(status),
                    options = statusLabels,
                    onSelect = { label -> status = statusValues[statusLabels.indexOf(label)] }
                )
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.admin2_vehicles_empty_title),
                    description = stringResource(R.string.admin2_vehicles_empty_desc)
                )
            } else {
                filtered.forEachIndexed { index, vehicle ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdminThumbnail(vehicle.imageUrl)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                vehicle.name,
                                fontFamily = DmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            AdminStatusBadge(vehicle.status)
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField(stringResource(R.string.admin2_seller), vehicle.seller, Modifier.weight(1.6f))
                            AdminField(stringResource(R.string.admin2_year), vehicle.year.toString(), Modifier.weight(0.7f))
                            AdminField(stringResource(R.string.admin2_price), vehicle.price, Modifier.weight(1f), bold = true)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AdminActionButton(stringResource(R.string.admin2_review), onClick = { selected = vehicle })
                        }
                    }
                }
            }
        }
    }

    selected?.let { vehicle ->
        AdminDetailModal(
            title = stringResource(R.string.admin2_vehicle_detail_title),
            subtitle = vehicle.name,
            onDismiss = { selected = null }
        ) {
            AdminDetailImage(vehicle.imageUrl)
            DetailRow(stringResource(R.string.admin2_vehicle), "${vehicle.name} ${vehicle.year}")
            DetailRow(stringResource(R.string.admin2_seller), vehicle.seller)
            DetailRow(stringResource(R.string.admin2_price), vehicle.price)
            DetailRow(stringResource(R.string.admin2_transmission), vehicle.transmision)
            DetailRow(stringResource(R.string.admin2_mileage), vehicle.kilometraje)
            DetailRow(stringResource(R.string.admin2_body_type), vehicle.tipo)
            DetailRow(stringResource(R.string.admin2_color), vehicle.color)
            DetailRow(stringResource(R.string.admin2_status), adminValueLabel(vehicle.status))
            AdminSheetActions {
                // Propuesta pendiente: aprobar publica el contenido; rechazar lo devuelve al vendedor.
                if (vehicle.pendingProposalId != null) {
                    AdminSheetButton(stringResource(R.string.admin2_approve), AdminButtonStyle.Positive) {
                        vm.moderate(vehicle, approve = true)
                        selected = null
                    }
                    AdminSheetButton(stringResource(R.string.admin2_reject), AdminButtonStyle.Destructive) {
                        vm.moderate(vehicle, approve = false)
                        selected = null
                    }
                } else if (vehicle.status != "Rechazado") {
                    AdminSheetButton(
                        stringResource(if (vehicle.enabledByAdmin) R.string.admin2_disable else R.string.admin2_enable),
                        if (vehicle.enabledByAdmin) AdminButtonStyle.Destructive else AdminButtonStyle.Positive
                    ) {
                        if (vehicle.enabledByAdmin) {
                            vm.dismissMessage()
                            disabling = vehicle
                        } else {
                            vm.setVehicleEnabled(vehicle, enabled = true)
                            selected = null
                        }
                    }
                }
                AdminSheetButton(stringResource(R.string.admin2_view_publication), AdminButtonStyle.Secondary) {
                    selected = null
                    onCarClick(vehicle.id)
                }
            }
        }
    }

    disabling?.let { vehicle ->
        DisableReasonDialog(
            working = vm.working,
            error = vm.message,
            onConfirm = { reason ->
                vm.setVehicleEnabled(vehicle, enabled = false, reason = reason) {
                    disabling = null
                    selected = null
                }
            },
            onDismiss = { if (!vm.working) disabling = null }
        )
    }
}
