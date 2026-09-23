package com.pame.karsy.feature.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun AdminUsersSection() {
    var search by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("Todos") }
    var selected by remember { mutableStateOf<AdminUser?>(null) }

    // TODO: cargar usuarios desde public.cuentas (con búsqueda y filtro por tipo en el servidor)
    val filtered = AdminSampleData.users.filter { user ->
        val matchesSearch = user.name.contains(search, ignoreCase = true) ||
            user.email.contains(search, ignoreCase = true)
        val matchesFilter = filter == "Todos" || user.type == filter
        matchesSearch && matchesFilter
    }

    AdminSectionScroll {
        AdminSectionHeader(
            title = "Usuarios",
            description = "Consulta y administra las cuentas registradas en Karsy."
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
                        placeholder = "Buscar por nombre o correo...",
                        modifier = Modifier.weight(1f)
                    )
                    AdminSelect(
                        value = filter,
                        options = listOf("Todos", "Particular", "Lote"),
                        onSelect = { filter = it }
                    )
                }
            }

            if (filtered.isEmpty()) {
                AdminEmptyState(
                    icon = Icons.Rounded.Search,
                    title = "No se encontraron usuarios",
                    description = "Prueba con otro nombre, correo o tipo de cuenta."
                )
            } else {
                filtered.forEachIndexed { index, user ->
                    AdminListRow(isLast = index == filtered.lastIndex) {
                        Row(verticalAlignment = Alignment.Top) {
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
                            AdminBadge(user.status, userStatusTone(user.status))
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AdminBadge(user.type, if (user.type == "Lote") BadgeTone.Info else BadgeTone.Neutral)
                            Text(
                                user.date,
                                fontFamily = DmSans,
                                fontSize = 12.sp,
                                color = AdminColors.TableText,
                                modifier = Modifier.weight(1f)
                            )
                            AdminActionButton("Ver detalle", onClick = { selected = user })
                        }
                    }
                }
            }
        }
    }

    selected?.let { user ->
        // TODO: suspender/reactivar cuenta: update public.cuentas set estado_cuenta='suspendida'
        //  + insertar en acciones_administrativas
        AdminDetailModal(
            title = "Detalle del usuario",
            subtitle = user.name,
            onDismiss = { selected = null }
        ) {
            DetailRow("Nombre", user.name)
            DetailRow("Tipo de cuenta", user.type)
            DetailRow("Estado", user.status)
            DetailRow("Publicaciones", user.posts.toString())
            DetailRow("Fecha de registro", user.date)
        }
    }
}

private fun userStatusTone(status: String) =
    if (status == "Activo") BadgeTone.Success else BadgeTone.Warning
