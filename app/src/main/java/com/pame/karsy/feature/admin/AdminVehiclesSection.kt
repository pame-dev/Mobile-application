package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy

@Composable
/**
 * "Revisar" abre la publicación con las acciones de admin abajo ([onReview]),
 * igual que la revisión de reportes.
 */
fun AdminVehiclesSection(vm: AdminViewModel, onReview: (Long) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var status by rememberSaveable { mutableStateOf("Todos") }

    val vehicles = vm.vehicles
    val filtered = vehicles.filter { vehicle ->
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
                AdminListHeader(
                    title = stringResource(R.string.admin2_listings_title),
                    count = pluralStringResource(R.plurals.admin2_listings_count, vehicles.size, vehicles.size)
                ) {
                    // Los estados se comparan en español; solo se traduce lo que se muestra.
                    AdminValueSelect(
                        status,
                        listOf("Todos", "Activo", "Pendiente", "Cambios pendientes", "Rechazado", "Deshabilitado", "Pausado", "Vendido")
                    ) { status = it }
                }
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = stringResource(R.string.admin2_vehicles_search),
                    modifier = Modifier.fillMaxWidth()
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
                        // Mismo formato que Reportes: tipo y estado, miniatura con título, campos y acción.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminBadge(vehicle.tipo, BadgeTone.Neutral)
                            Spacer(Modifier.weight(1f))
                            AdminStatusBadge(vehicle.status)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AdminThumbnail(vehicle.imageUrl, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    vehicle.name,
                                    fontFamily = DmSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KarsyNavy
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${vehicle.year} · ${vehicle.price}",
                                    fontFamily = DmSans,
                                    fontSize = 12.sp,
                                    color = AdminColors.TableText
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField(stringResource(R.string.admin2_seller), vehicle.seller, Modifier.weight(1.3f))
                            AdminField(stringResource(R.string.admin2_mileage), vehicle.kilometraje, Modifier.weight(1f))
                            AdminActionButton(stringResource(R.string.admin2_review), onClick = { onReview(vehicle.id) })
                        }
                    }
                }
            }
        }
    }
}
