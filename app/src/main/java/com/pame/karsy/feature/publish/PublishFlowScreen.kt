package com.pame.karsy.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.feature.publish.steps.ConfirmStep
import com.pame.karsy.feature.publish.steps.DatosStep
import com.pame.karsy.feature.publish.steps.DetailsStep
import com.pame.karsy.feature.publish.steps.PhotosStep

/**
 * Flujo "Publicar vehículo": 4 pasos (Datos, Fotos, Detalles, Confirmar) manejados con
 * estado local y overlay de éxito al publicar.
 */
@Composable
fun PublishFlowScreen(onBack: () -> Unit, onFinished: () -> Unit) {
    val vm: PublishViewModel = viewModel()
    val context = LocalContext.current

    val goBack = { if (!vm.back()) onBack() }

    // El botón atrás del sistema regresa al paso anterior (paso 1 -> sale del flujo).
    BackHandler(enabled = !vm.published, onBack = goBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        when (vm.step) {
            1 -> DatosStep(form = vm, onNext = vm::next, onBack = goBack)
            2 -> PhotosStep(form = vm, onNext = vm::next, onBack = goBack)
            3 -> DetailsStep(form = vm, onNext = vm::next, onBack = goBack)
            else -> ConfirmStep(form = vm, onPublish = { vm.publish(context) }, onBack = goBack)
        }

        AnimatedVisibility(visible = vm.published, enter = fadeIn(), exit = fadeOut()) {
            // NavGraph saca este flujo de la pila al navegar al panel.
            SuccessOverlay(title = vm.titulo, onDone = onFinished)
        }
    }
}
