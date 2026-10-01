package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun AdminUsersSection(vm: AdminViewModel) {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }

    val filtered = vm.users.filter { user ->
        val matchesSearch = user.name.contains(search, ignoreCase = true) ||
            user.email.contains(search, ignoreCase = true)
        val matchesFilter = filter == "Todos" || user.type == filter
        matchesSearch && matchesFilter
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = stringResource(R.string.admin2_section_users),
            description = stringResource(R.string.admin2_users_desc)
        )
        AdminCard {
            AdminFilterBar {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdminSearchBar(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = stringResource(R.string.admin2_users_search),
                        modifier = Modifier.weight(1f)
                    )
                    // Los valores del filtro se comparan en español; solo se traduce lo que se muestra.
                    val filterValues = listOf("Todos", "Particular", "Lote")
                    val filterLabels = filterValues.map { adminValueLabel(it) }
                    AdminSelect(
                        value = adminValueLabel(filter),
                        options = filterLabels,
                        onSelect = { label -> filter = filterValues[filterLabels.indexOf(label)] }
                    )
                }
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileAvatar(name = user.name, avatarUrl = null, size = 40.dp, initialsSize = 14.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    user.name,
                                    fontFamily = DmSans,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KarsyNavy
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(user.email, fontFamily = DmSans, fontSize = 11.sp, color = AdminColors.Muted)
                            }
                            AdminStatusBadge(user.status)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminBadge(adminValueLabel(user.type), if (user.type == "Lote") BadgeTone.Info else BadgeTone.Neutral)
                            if (user.isAdmin) AdminBadge("Admin", BadgeTone.Info)
                            Text(
                                user.date,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                color = AdminColors.TableText,
                                modifier = Modifier.weight(1f)
                            )
                            AdminActionButton(stringResource(R.string.admin2_view_detail), onClick = { selected = user })
                        }
                    }
                }
            }
        }
    }

    selected?.let { user ->
        AdminDetailModal(
            title = stringResource(R.string.admin2_user_detail_title),
            subtitle = user.name,
            onDismiss = { selected = null }
        ) {
            val typeLabel = adminValueLabel(user.type)
            DetailRow(stringResource(R.string.admin2_name), user.name)
            DetailRow(stringResource(R.string.admin2_email), user.email)
            DetailRow(
                stringResource(R.string.admin2_account_type),
                if (user.isAdmin) stringResource(R.string.admin2_type_admin, typeLabel) else typeLabel
            )
            DetailRow(stringResource(R.string.admin2_status), adminValueLabel(user.status))
            DetailRow(stringResource(R.string.admin2_legend_publications), user.posts.toString())
            DetailRow(stringResource(R.string.admin2_registration_date), user.date)
            // Suspender / reactivar (admin_cambiar_estado_cuenta registra la acción en la bitácora).
            if (user.id != SessionManager.userId) {
                AdminSheetActions {
                    if (user.status == "Suspendido") {
                        AdminSheetButton(stringResource(R.string.admin2_reactivate_account), AdminButtonStyle.Positive) {
                            vm.setSuspended(user.id, suspend = false)
                            selected = null
                        }
                    } else {
                        AdminSheetButton(stringResource(R.string.admin2_suspend_account), AdminButtonStyle.Destructive) {
                            vm.setSuspended(user.id, suspend = true)
                            selected = null
                        }
                    }
                }
            }
        }
    }
}
