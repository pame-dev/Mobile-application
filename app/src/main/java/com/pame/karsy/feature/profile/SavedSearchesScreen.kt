package com.pame.karsy.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pame.karsy.R
import com.pame.karsy.core.components.AvisoKarsy
import com.pame.karsy.core.components.KarsyPullToRefresh
import com.pame.karsy.core.components.PrimaryButton
import com.pame.karsy.core.components.SkeletonListRow
import com.pame.karsy.core.components.SubHeader
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealLight
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.karsyTint
import com.pame.karsy.core.util.Formato
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.BusquedaGuardadaDto
import com.pame.karsy.data.repository.SearchRepository
import kotlinx.coroutines.launch

/**
 * Búsquedas guardadas (Configuración). Se crean en el inicio con "Avisarme de nuevos"
 * y avisan cuando se publica un auto que coincide.
 */
@Composable
fun SavedSearchesScreen(onBack: () -> Unit, onExplore: () -> Unit) {
    val scope = rememberCoroutineScope()
    var busquedas by remember { mutableStateOf<List<BusquedaGuardadaDto>?>(null) }
    suspend fun load() {
        busquedas = safeCall { SearchRepository.mine() }.getOrDefault(emptyList())
    }
    LaunchedEffect(Unit) { load() }

    fun borrar(b: BusquedaGuardadaDto) {
        scope.launch {
            safeCall { SearchRepository.delete(b.id) }
                .onSuccess { busquedas = busquedas?.filter { it.id != b.id } }
                .onFailure { AvisoKarsy.mostrar(it.mensajeUsuario(), error = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        SubHeader(
            title = stringResource(R.string.saved_search_title),
            onBack = onBack,
            modifier = Modifier.shadow(3.dp, ambientColor = CardShadow, spotColor = CardShadow)
        )
        KarsyPullToRefresh(onRefresh = { done -> scope.launch { load(); done() } }, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 40.dp)
            ) {
                Text(
                    stringResource(R.string.saved_search_hint),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Body,
                    lineHeight = 18.sp,
                    color = KarsyMid,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 14.dp)
                )
                val items = busquedas
                ProfileCard {
                    when {
                        items == null -> Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            repeat(3) { SkeletonListRow() }
                        }
                        items.isEmpty() -> Column {
                            Text(
                                stringResource(R.string.saved_search_empty),
                                fontFamily = DmSans,
                                fontSize = KarsyTextSize.BodyLarge,
                                color = KarsyMid,
                                modifier = Modifier.padding(20.dp)
                            )
                            PrimaryButton(
                                text = stringResource(R.string.core_empty_explore),
                                onClick = onExplore,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                            )
                        }
                        else -> Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(14.dp)
                        ) {
                            items.forEach { b -> SavedSearchRow(b, onDelete = { borrar(b) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedSearchRow(b: BusquedaGuardadaDto, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KarsyRadius.Md))
            .background(karsyTint(KarsyTeal, KarsyTealLight))
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(KarsyRadius.Sm))
                .background(KarsySurface)
        ) {
            Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = KarsyTeal, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                SearchRepository.describir(b),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 18.sp,
                color = KarsyCharcoal
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.saved_search_since, Formato.haceCuanto(b.fecha)),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Caption,
                color = KarsyMid
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onDelete)
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.saved_search_delete),
                tint = KarsyMid,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
