package com.pame.karsy.feature.dashboard

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pame.karsy.R
import com.pame.karsy.core.components.ConstanciaField
import com.pame.karsy.core.locale.texto
import com.pame.karsy.core.supabase.esDuplicado
import com.pame.karsy.core.supabase.mensajeUsuario
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsyRadius
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyBg
import com.pame.karsy.core.theme.KarsyBorderMuted
import com.pame.karsy.core.theme.KarsyCharcoal
import com.pame.karsy.core.theme.KarsyError
import com.pame.karsy.core.theme.KarsyMid
import com.pame.karsy.core.theme.KarsyNavy
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyTealText
import com.pame.karsy.core.theme.KarsyTextSize
import com.pame.karsy.core.theme.KarsyWarningLight
import com.pame.karsy.core.theme.KarsyWhite
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.core.util.ArchivoConstancia
import com.pame.karsy.core.util.Constancia
import com.pame.karsy.core.util.PagoDestacado
import com.pame.karsy.core.util.copiarAlPortapapeles
import com.pame.karsy.core.util.safeCall
import com.pame.karsy.data.remote.DatosPagoDto
import com.pame.karsy.data.repository.CarRepository
import kotlinx.coroutines.launch

/**
 * Estado de la ventana "Destacar tu auto": datos para pagar (datos_pago_destacado), el
 * comprobante elegido y la casilla de "no reembolsable". Lo tienen DashboardViewModel y
 * CarDetailViewModel, que piden destacar desde Mi panel y desde la publicación.
 */
class DestacarPago {
    var datos by mutableStateOf<DatosPagoDto?>(null)
        private set
    var cargando by mutableStateOf(false)
        private set
    /** No se pudieron leer los datos de pago. */
    var errorCarga by mutableStateOf<String?>(null)
        private set
    var comprobante by mutableStateOf<ArchivoConstancia?>(null)
        private set
    var comprobanteError by mutableStateOf<String?>(null)
        private set
    var aceptaNoReembolso by mutableStateOf(false)
    var enviando by mutableStateOf(false)
        private set
    /** Error al subir el comprobante o al pedir el destacado. */
    var error by mutableStateOf<String?>(null)
        private set

    /** Hay banco y CLABE: se puede pagar y pedir. */
    val disponible: Boolean get() = datos?.let { PagoDestacado.disponible(it.banco, it.clabe) } == true

    /** "Enviar solicitud" se activa con comprobante y la casilla marcada. */
    val listo: Boolean get() = disponible && comprobante != null && aceptaNoReembolso && !enviando

    /** Al abrir la ventana: limpia lo anterior y vuelve a leer precio, días y cuenta (el admin los puede cambiar). */
    fun abrir(vm: ViewModel) {
        comprobante = null
        comprobanteError = null
        aceptaNoReembolso = false
        error = null
        // Sin los datos viejos: si la lectura falla se ve el error y Reintentar, no una CLABE vieja.
        datos = null
        cargar(vm)
    }

    fun cargar(vm: ViewModel) {
        if (cargando) return
        cargando = true
        errorCarga = null
        vm.viewModelScope.launch {
            safeCall { CarRepository.datosPagoDestacado() }
                .onSuccess { datos = it }
                .onFailure { errorCarga = it.mensajeUsuario() }
            cargando = false
        }
    }

    /** Mismas reglas que la constancia: JPG, PNG o PDF de máximo 5 MB; si no sirve se avisa en el campo. */
    fun elegir(context: Context, uri: Uri) {
        runCatching { Constancia.revisar(context, uri) }
            .onSuccess {
                comprobante = it
                comprobanteError = null
            }
            .onFailure { comprobanteError = it.mensajeUsuario() }
    }

    fun quitar() {
        comprobante = null
        comprobanteError = null
    }

    /** Sube el comprobante y pide el destacado (solicitar_destacado); [onSent] solo si la BD lo aceptó. */
    fun enviar(vm: ViewModel, context: Context, idPublicacion: Long, onSent: () -> Unit) {
        val archivo = comprobante ?: return
        if (!listo) return
        val appContext = context.applicationContext
        enviando = true
        error = null
        vm.viewModelScope.launch {
            safeCall {
                val ruta = CarRepository.uploadComprobante(Constancia.bytes(appContext, archivo), archivo.mime)
                CarRepository.requestFeatured(idPublicacion, ruta)
            }
                .onSuccess { onSent() }
                .onFailure {
                    error = if (it.esDuplicado()) texto(R.string.profile_dashboard_request_already_pending) else it.mensajeUsuario()
                }
            enviando = false
        }
    }
}

/**
 * Hoja inferior "Destacar tu auto" (solo lotes): precio, cuenta para transferir con la
 * referencia KAR-<id>, el comprobante y la casilla de "no reembolsable". Destacar por primera
 * vez y renovar usan la misma ventana (renovar también se paga).
 */
@Composable
fun DestacarDialog(
    carName: String,
    idPublicacion: Long,
    pago: DestacarPago,
    onRetry: () -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    // Mientras se envía no se cierra: así no se pierde la respuesta.
    DashboardSheet(onDismiss = { if (!pago.enviando) onCancel() }) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Ícono de estrella con halo
            Box(
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .size(88.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(KarsyTeal.copy(alpha = 0.35f), KarsyTeal.copy(alpha = 0f))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(KarsyTealSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KarsyTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = KarsyWhite, modifier = Modifier.size(28.dp))
                    }
                }
            }

            Text(
                stringResource(R.string.profile_feature_dialog_title),
                fontFamily = Outfit,
                fontSize = KarsyTextSize.Headline,
                fontWeight = FontWeight.Bold,
                color = KarsyInk,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                carName,
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                fontWeight = FontWeight.SemiBold,
                color = KarsyTealText,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .clip(CircleShape)
                    .background(KarsyTealSoft)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            val datos = pago.datos
            when {
                datos == null && pago.cargando -> CircularProgressIndicator(
                    color = KarsyTeal,
                    modifier = Modifier.padding(vertical = 24.dp).size(32.dp)
                )
                datos == null -> {
                    Text(
                        pago.errorCarga ?: stringResource(R.string.core_error_generic),
                        fontFamily = DmSans,
                        fontSize = KarsyTextSize.Body,
                        color = KarsyError,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.featured_pay_retry), fontFamily = Outfit, fontWeight = FontWeight.Bold, color = KarsyTealText)
                    }
                }
                else -> {
                    val dias = pluralStringResource(R.plurals.featured_pay_days, datos.dias, datos.dias)
                    Text(
                        stringResource(R.string.profile_feature_dialog_body, dias),
                        fontFamily = DmSans,
                        fontSize = KarsyTextSize.Body,
                        lineHeight = 21.sp,
                        color = KarsyCharcoal,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // Precio grande: "$300.00 MXN" · "30 días".
                    Text(
                        stringResource(R.string.featured_pay_price, PagoDestacado.monto(datos.precio)),
                        fontFamily = Outfit,
                        fontSize = KarsyTextSize.DisplayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = KarsyInk
                    )
                    Text(
                        dias,
                        fontFamily = DmSans,
                        fontSize = KarsyTextSize.Body,
                        fontWeight = FontWeight.SemiBold,
                        color = KarsyMid,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    HorizontalDivider(thickness = 1.dp, color = KarsyBg, modifier = Modifier.padding(bottom = 20.dp))

                    if (!pago.disponible) {
                        // Sin banco o CLABE nadie puede pagar: solo el aviso.
                        Text(
                            stringResource(R.string.featured_pay_unavailable),
                            fontFamily = DmSans,
                            fontSize = KarsyTextSize.Body,
                            lineHeight = 19.sp,
                            color = KarsyInk,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(KarsyRadius.Md))
                                .background(KarsyWarningLight)
                                .padding(12.dp)
                        )
                    } else {
                        PasosDePago(
                            datos = datos,
                            referencia = PagoDestacado.referencia(idPublicacion),
                            pago = pago,
                            onPick = { pago.elegir(context, it) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (datos != null && pago.disponible) {
                    pago.error?.let {
                        Text(it, fontFamily = DmSans, fontSize = KarsyTextSize.Body, color = KarsyError, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    }
                    SheetPrimaryButton(
                        text = stringResource(if (pago.enviando) R.string.featured_pay_sending else R.string.profile_feature_dialog_confirm),
                        onClick = onSend,
                        fontSize = 15,
                        enabled = pago.listo
                    )
                }
                TextButton(
                    onClick = onCancel,
                    enabled = !pago.enviando,
                    shape = CircleShape,
                    contentPadding = PaddingValues(vertical = 13.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.profile_cancel), fontFamily = Outfit, fontSize = KarsyTextSize.BodyLarge, fontWeight = FontWeight.SemiBold, color = KarsyMid)
                }
            }
        }
    }
}

/** Los tres pasos: transferir a la cuenta, escribir la referencia y subir el comprobante; luego la casilla. */
@Composable
private fun PasosDePago(datos: DatosPagoDto, referencia: String, pago: DestacarPago, onPick: (Uri) -> Unit) {
    val context = LocalContext.current
    val copiado = stringResource(R.string.featured_pay_copied)
    val etiquetaClabe = stringResource(R.string.featured_pay_clabe)
    val etiquetaReferencia = stringResource(R.string.featured_pay_reference)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Paso(1, stringResource(R.string.featured_pay_step1, PagoDestacado.monto(datos.precio))) {
            CajaDato {
                Text(
                    stringResource(R.string.featured_pay_bank_value, datos.banco.orEmpty()),
                    fontFamily = DmSans,
                    fontSize = KarsyTextSize.Body,
                    color = KarsyCharcoal
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(etiquetaClabe, fontFamily = DmSans, fontSize = KarsyTextSize.Small, color = KarsyMid)
                        SelectionContainer {
                            Text(datos.clabe.orEmpty(), fontFamily = FontFamily.Monospace, fontSize = KarsyTextSize.Subtitle, fontWeight = FontWeight.Bold, color = KarsyInk)
                        }
                    }
                    BotonCopiar(stringResource(R.string.featured_pay_copy_cd, etiquetaClabe)) {
                        // Dato bancario: marcado como sensible, igual que la CURP.
                        copiarAlPortapapeles(context, etiquetaClabe, datos.clabe.orEmpty(), copiado, sensible = true)
                    }
                }
            }
            Text(
                stringResource(R.string.featured_pay_holder_note),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Small,
                lineHeight = 17.sp,
                color = KarsyMid,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Paso(2, stringResource(R.string.featured_pay_step2)) {
            CajaDato {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer(Modifier.weight(1f)) {
                        Text(referencia, fontFamily = FontFamily.Monospace, fontSize = KarsyTextSize.TitleLarge, fontWeight = FontWeight.Bold, color = KarsyInk)
                    }
                    BotonCopiar(stringResource(R.string.featured_pay_copy_cd, etiquetaReferencia)) {
                        copiarAlPortapapeles(context, etiquetaReferencia, referencia, copiado)
                    }
                }
            }
        }
        Paso(3, stringResource(R.string.featured_pay_step3)) {
            ConstanciaField(
                archivo = pago.comprobante,
                error = pago.comprobanteError,
                onPick = onPick,
                onRemove = pago::quitar,
                label = stringResource(R.string.featured_pay_receipt_label, Constancia.MAX_MB),
                showHelp = false,
                enabled = !pago.enviando
            )
        }

        Text(
            stringResource(R.string.featured_pay_no_refund_notice),
            fontFamily = DmSans,
            fontSize = KarsyTextSize.Small,
            lineHeight = 17.sp,
            color = KarsyInk,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(KarsyRadius.Md))
                .background(KarsyWarningLight)
                .padding(12.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = pago.aceptaNoReembolso,
                    enabled = !pago.enviando,
                    role = Role.Checkbox,
                    onValueChange = { pago.aceptaNoReembolso = it }
                )
        ) {
            Checkbox(
                checked = pago.aceptaNoReembolso,
                onCheckedChange = null,
                enabled = !pago.enviando,
                colors = CheckboxDefaults.colors(checkedColor = KarsyTeal)
            )
            Text(
                stringResource(R.string.featured_pay_no_refund_check),
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                color = KarsyInk,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        Text(
            stringResource(R.string.featured_pay_review_time),
            fontFamily = DmSans,
            fontSize = KarsyTextSize.Small,
            color = KarsyMid,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** Paso numerado con su texto y lo que va debajo. */
@Composable
private fun Paso(numero: Int, texto: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KarsyNavy)
            ) {
                Text(numero.toString(), fontFamily = Outfit, fontSize = KarsyTextSize.Small, fontWeight = FontWeight.Bold, color = KarsyWhite)
            }
            Text(
                texto,
                fontFamily = DmSans,
                fontSize = KarsyTextSize.Body,
                fontWeight = FontWeight.SemiBold,
                color = KarsyInk,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        content()
    }
}

@Composable
private fun CajaDato(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(KarsyRadius.Md))
            .background(KarsyBg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun BotonCopiar(descripcion: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        shape = CircleShape,
        // TalkBack dice "Copiar CLABE" o "Copiar referencia" en lugar de solo "Copiar".
        modifier = Modifier
            .padding(start = 8.dp)
            .semantics { contentDescription = descripcion }
    ) {
        Text(
            stringResource(R.string.featured_pay_copy),
            fontFamily = Outfit,
            fontSize = KarsyTextSize.Body,
            fontWeight = FontWeight.Bold,
            color = KarsyTealText
        )
    }
}

/**
 * Contenedor común de las hojas del panel: fondo oscurecido que cierra al tocarlo y
 * tarjeta blanca redondeada anclada abajo con su "agarradera".
 */
@Composable
internal fun DashboardSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KarsyNavy.copy(alpha = 0.52f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            // La hoja de destacar es alta: que no quede debajo de la barra de estado.
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(KarsyRadius.Xxl), ambientColor = KarsyNavy, spotColor = KarsyNavy)
                .clip(RoundedCornerShape(KarsyRadius.Xxl))
                .background(KarsySurface)
                // Evita que un toque dentro de la tarjeta la cierre.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(KarsyBorderMuted)
            )
            content()
        }
    }
}

@Composable
internal fun SheetPrimaryButton(text: String, onClick: () -> Unit, fontSize: Int, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = KarsyNavy, contentColor = KarsyWhite),
        contentPadding = PaddingValues(vertical = if (fontSize >= 15) 15.dp else 14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text, fontFamily = Outfit, fontSize = fontSize.sp, fontWeight = FontWeight.Bold)
    }
}
