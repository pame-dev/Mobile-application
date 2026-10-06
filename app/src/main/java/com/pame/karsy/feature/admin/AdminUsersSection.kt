package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.session.SessionManager
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.feature.profile.ProfileAvatar

/** Cuentas particulares y lotes en una sola lista (los lotes ya no tienen sección propia). */
@Composable
fun AdminUsersSection(vm: AdminViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }
    // Cuenta que se va a suspender (abre el diálogo de motivo).
    var suspending by remember { mutableStateOf<AdminUser?>(null) }

    val users = vm.users
    val filtered = users.filter { user ->
        val matchesSearch = "${user.name} ${user.email} ${user.responsible} ${user.location}"
            .contains(search, ignoreCase = true)
        val matchesFilter = when (filter) {
            "Todos" -> true
            "Particular", "Lote" -> user.type == filter
            else -> user.status == filter
        }
        matchesSearch && matchesFilter
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_section_users),
            description = stringResource(R.string.admin2_users_desc)
        )
        AdminCard {
            AdminFilterBar {
                AdminListHeader(
                    title = stringResource(R.string.admin2_accounts_title),
                    count = pluralStringResource(R.plurals.admin2_accounts_count, users.size, users.size)
                ) {
                    AdminValueSelect(filter, listOf("Todos", "Particular", "Lote", "Activo", "Suspendido")) { filter = it }
                }
                AdminSearchBar(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = stringResource(R.string.admin2_users_search),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = stringResource(R.string.admin2_users_empty_title),
                    description = stringResource(R.string.admin2_users_empty_desc)
                )
            } else {
                filtered.forEachIndexed { index, user ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        AdminUserRowContent(user) {
                            AdminActionButton(stringResource(R.string.admin2_view_detail), onClick = { selected = user })
                        }
                    }
                }
            }
        }
    }

    selected?.let { user ->
        val isLot = user.type == "Lote"
        AdminDetailModal(
            title = stringResource(if (isLot) R.string.admin2_lot_detail_title else R.string.admin2_user_detail_title),
            subtitle = user.name,
            onDismiss = { selected = null }
        ) {
            AdminUserDetailRows(user)
            // Suspender / reactivar (admin_cambiar_estado_cuenta registra la acción en la bitácora).
            if (user.id != SessionManager.userId) {
                AdminSheetActions {
                    if (user.status == "Suspendido") {
                        AdminSheetButton(
                            stringResource(if (isLot) R.string.admin2_reactivate_lot else R.string.admin2_reactivate_account),
                            AdminButtonStyle.Positive
                        ) {
                            vm.setSuspended(user.id, suspend = false)
                            selected = null
                        }
                    } else {
                        AdminSheetButton(
                            stringResource(if (isLot) R.string.admin2_suspend_lot else R.string.admin2_suspend_account),
                            AdminButtonStyle.Destructive
                        ) {
                            vm.dismissMessage()
                            suspending = user
                        }
                    }
                }
            }
        }
    }

    suspending?.let { user ->
        AdminReasonDialog(
            working = vm.working,
            error = vm.message,
            texts = if (user.type == "Lote") ReasonDialogTexts.SuspendLot else ReasonDialogTexts.SuspendAccount,
            onConfirm = { reason ->
                vm.setSuspended(user.id, suspend = true, reason = reason) {
                    suspending = null
                    selected = null
                }
            },
            onDismiss = { if (!vm.working) suspending = null }
        )
    }
}

/**
 * Fila de cuenta con el formato de Reportes: tipo y estado arriba, avatar con nombre,
 * y campos con la acción ([action]) abajo. También la usa la revisión de reportes de cuenta.
 */
@Composable
fun AdminUserRowContent(user: AdminUser, action: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AdminBadge(adminValueLabel(user.type), if (user.type == "Lote") BadgeTone.Info else BadgeTone.Neutral)
        if (user.isAdmin) AdminBadge("Admin", BadgeTone.Info)
        Spacer(Modifier.weight(1f))
        AdminStatusBadge(user.status)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        ProfileAvatar(name = user.name, avatarUrl = null, size = 44.dp, initialsSize = 15.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(user.name, fontFamily = DmSans, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
            Spacer(Modifier.height(2.dp))
            Text(
                user.email,
                fontFamily = DmSans,
                fontSize = 11.sp,
                color = AdminColors.Muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (user.type == "Lote") {
            AdminField(stringResource(R.string.admin2_location), user.location.ifBlank { "—" }, Modifier.weight(1.3f))
        } else {
            AdminField(stringResource(R.string.admin2_legend_publications), user.posts.toString(), Modifier.weight(1.3f))
        }
        AdminField(stringResource(R.string.admin2_date), user.date, Modifier.weight(1f))
        action?.invoke()
    }
}

/** Datos de la cuenta en el detalle; los lotes agregan responsable, ubicación y vehículos activos. */
@Composable
fun AdminUserDetailRows(user: AdminUser) {
    val typeLabel = adminValueLabel(user.type)
    DetailRow(stringResource(R.string.admin2_name), user.name)
    DetailRow(stringResource(R.string.admin2_email), user.email)
    DetailRow(
        stringResource(R.string.admin2_account_type),
        if (user.isAdmin) stringResource(R.string.admin2_type_admin, typeLabel) else typeLabel
    )
    DetailRow(stringResource(R.string.admin2_status), adminValueLabel(user.status))
    if (user.type == "Lote") {
        DetailRow(stringResource(R.string.admin2_responsible), user.responsible.ifBlank { "—" })
        DetailRow(stringResource(R.string.admin2_location), user.location.ifBlank { "—" })
        DetailRow(stringResource(R.string.admin2_active_vehicles), user.activeVehicles.toString())
    }
    DetailRow(stringResource(R.string.admin2_legend_publications), user.posts.toString())
    DetailRow(stringResource(R.string.admin2_registration_date), user.date)
}
