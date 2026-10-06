package com.pame.karsy.core.components

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal

/**
 * Recargar deslizando hacia abajo. [onRefresh] recibe la función que hay que llamar
 * cuando terminó de recargar (así la ruedita se quita justo cuando hay datos nuevos).
 * Funciona con listas lazy y con columnas con verticalScroll.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KarsyPullToRefresh(
    onRefresh: (done: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    var refreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            refreshing = true
            onRefresh { refreshing = false }
        },
        state = state,
        modifier = modifier,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = refreshing,
                containerColor = KarsySurface,
                color = KarsyTeal,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        content = content
    )
}
