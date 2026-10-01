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
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.feature.profile.ProfileAvatar

@Composable
fun AdminLotesSection(vm: AdminViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<AdminLot?>(null) }
    // Lote que se va a suspender (abre el diálogo de motivo).
    var suspending by remember { mutableStateOf<AdminLot?>(null) }

    val filtered = vm.lots.filter { lot ->
        "${lot.name} ${lot.responsible} ${lot.city}".contains(search, ignoreCase = true)
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_section_lots),
            description = stringResource(R.string.admin2_lots_desc)
        )
        AdminCard {
            AdminFilterBar {
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = stringResource(R.string.admin2_lots_search),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.admin2_lots_empty_title),
                    description = stringResource(R.string.admin2_lots_empty_desc)
                )
            } else {
                filtered.forEachIndexed { index, lot ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(name = lot.name, avatarUrl = null, size = 40.dp, initialsSize = 14.sp)
                            Spacer(Modifier.width(12.dp))
                            Text(
                                lot.name,
                                fontFamily = DmSans,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KarsyNavy,
                                modifier = Modifier.weight(1f)
                            )
                            AdminStatusBadge(lot.status)
                        }
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminField(stringResource(R.string.admin2_responsible), lot.responsible, Modifier.weight(1.3f))
                            AdminField(stringResource(R.string.admin2_location), lot.city, Modifier.weight(1.2f))
                            AdminField(stringResource(R.string.admin2_section_vehicles), lot.vehicles.toString(), Modifier.weight(0.8f), bold = true)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            AdminActionButton(stringResource(R.string.admin2_view_lot), onClick = { selected = lot })
                        }
                    }
                }
            }
        }
    }

    selected?.let { lot ->
        AdminDetailModal(
            title = stringResource(R.string.admin2_lot_detail_title),
            subtitle = lot.name,
            onDismiss = { selected = null }
        ) {
            DetailRow(stringResource(R.string.admin2_lot_name), lot.name)
            DetailRow(stringResource(R.string.admin2_responsible), lot.responsible)
            DetailRow(stringResource(R.string.admin2_city), lot.city)
            DetailRow(stringResource(R.string.admin2_active_vehicles), lot.vehicles.toString())
            DetailRow(stringResource(R.string.admin2_status), adminValueLabel(lot.status))
            if (lot.id != SessionManager.userId) {
                AdminSheetActions {
                    val suspendido = lot.status == "Suspendido"
                    AdminSheetButton(
                        stringResource(if (suspendido) R.string.admin2_reactivate_lot else R.string.admin2_suspend_lot),
                        if (suspendido) AdminButtonStyle.Positive else AdminButtonStyle.Destructive
                    ) {
                        if (suspendido) {
                            vm.setSuspended(lot.id, suspend = false)
                            selected = null
                        } else {
                            vm.dismissMessage()
                            suspending = lot
                        }
                    }
                }
            }
        }
    }

    suspending?.let { lot ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message,
            texts = ReasonDialogTexts.SuspendLot,
            onConfirm = { reason ->
                vm.setSuspended(lot.id, suspend = true, reason = reason) {
                    suspending = null
                    selected = null
                }
            },
            onDismiss = { if (!vm.working) suspending = null }
        )
    }
}
