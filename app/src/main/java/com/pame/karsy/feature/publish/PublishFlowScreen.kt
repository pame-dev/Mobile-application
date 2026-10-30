package com.pame.karsy.feature.publish

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pame.karsy.R
import com.pame.karsy.core.components.ConfirmDialog
import com.pame.karsy.core.components.SkeletonBlock
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyRadius
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

    var confirmExit by rememberSaveable { mutableStateOf(false) }

    // En el paso 1 "atrás" sale del flujo y se pierde lo capturado: se pide confirmación.
    val goBack = { if (!vm.back()) confirmExit = true }

    // El botón atrás del sistema regresa al paso anterior (paso 1 -> sale del flujo).
    BackHandler(enabled = !vm.published, onBack = goBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyBg)
    ) {
        if (vm.loadingEdit) {
            // Cargando los datos de la publicación rechazada que se va a corregir.
            PublishSkeleton()
        } else when (vm.step) {
            1 -> DatosStep(form = vm, onNext = { vm.next(context) }, onBack = goBack)
            2 -> PhotosStep(form = vm, onNext = { vm.next(context) }, onBack = goBack)
            3 -> DetailsStep(form = vm, onNext = { vm.next(context) }, onBack = goBack)
            else -> ConfirmStep(form = vm, onPublish = { vm.publish(context) }, onBack = goBack)
        }

        AnimatedVisibility(visible = vm.published, enter = fadeIn(), exit = fadeOut()) {
            // NavGraph saca este flujo de la pila al navegar al panel.
            SuccessOverlay(title = vm.titulo, onDone = onFinished, resubmitted = vm.isEditing, approvedEdit = vm.editingApproved)
        }

        ConfirmDialog(
            visible = confirmExit,
            icon = Icons.Rounded.Warning,
            accent = KarsyError,
            title = stringResource(R.string.publish_exit_title),
            message = stringResource(if (vm.isEditing) R.string.publish_exit_edit_message else R.string.publish_exit_message),
            confirmText = stringResource(R.string.publish_exit_yes),
            onCancel = { confirmExit = false },
            onConfirm = {
                confirmExit = false
                onBack()
            }
        )
    }
}

/** Forma del formulario (título, pasos y campos) mientras carga la publicación a corregir. */
@Composable
private fun PublishSkeleton() {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SkeletonBlock(26.dp, width = 200.dp)
        SkeletonBlock(8.dp, shape = CircleShape)
        Spacer(Modifier.height(8.dp))
        repeat(5) {
            SkeletonBlock(12.dp, width = 110.dp)
            SkeletonBlock(48.dp, shape = RoundedCornerShape(KarsyRadius.Md))
        }
    }
}
