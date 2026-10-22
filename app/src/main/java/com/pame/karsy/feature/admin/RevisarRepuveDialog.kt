package com.pame.karsy.feature.admin

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.pame.karsy.R
import com.pame.karsy.core.theme.DmSans
import com.pame.karsy.core.theme.KarsyInk
import com.pame.karsy.core.theme.KarsySurface
import com.pame.karsy.core.theme.KarsyTeal
import com.pame.karsy.core.theme.KarsyTealSoft
import com.pame.karsy.core.theme.KarsyWarningLight
import com.pame.karsy.core.theme.Outfit
import com.pame.karsy.data.remote.DocumentoVehiculoDto
import com.pame.karsy.feature.publish.PublishViewModel

/** Consulta ciudadana del Registro Público Vehicular (por NIV o placa). */
private const val REPUVE = "https://www2.repuve.gob.mx:8443/ciudadania/"

/**
 * Revisar una publicación pendiente en REPUVE antes de aprobarla (igual que "Revisar CURP"):
 * guía, el NIV o la placa para copiar (o el archivo de la tarjeta / baja para leerlos), el
 * vehículo y año de la publicación para comparar, y dos casillas que activan Aprobar.
 * Sin documento ([document] null: publicaciones anteriores) se avisa y basta con una casilla.
 * Mientras [loaded] es false (el documento aún se lee) no se puede aprobar.
 * Rechazar pide el motivo (como siempre).
 */
@Composable
internal fun RevisarRepuveDialog(
    vehicle: AdminVehicle,
    loaded: Boolean,
    document: DocumentoVehiculoDto?,
    working: Boolean,
    error: String?,
    onOpenFile: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var coincide by rememberSaveable { mutableStateOf(false) }
    var sinRobo by rememberSaveable { mutableStateOf(false) }
    var revisada by rememberSaveable { mutableStateOf(false) }
    val escrito = document?.tipo == "vin" || document?.tipo == "placa"
    val listo = loaded && !working && if (document == null) revisada else coincide && sinRobo
    val copiado = stringResource(R.string.admin2_repuve_copied)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KarsySurface,
        title = {
            Text(
                stringResource(R.string.admin2_repuve_title, "${vehicle.name} ${vehicle.year}"),
                fontFamily = Outfit,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = KarsyInk
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.admin2_repuve_sub), fontFamily = DmSans, fontSize = 13.sp, color = AdminColors.NeutralText)
                if (!loaded) {
                    LinearProgressIndicator(color = KarsyTeal, modifier = Modifier.fillMaxWidth())
                } else if (document == null) {
                    Text(
                        stringResource(R.string.admin2_repuve_no_document),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = KarsyInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KarsyWarningLight)
                            .padding(12.dp)
                    )
                } else {
                    Text(
                        stringResource(if (escrito) R.string.admin2_repuve_steps_copy else R.string.admin2_repuve_steps_file),
                        fontFamily = DmSans,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = KarsyInk,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KarsyTealSoft)
                            .padding(12.dp)
                    )
                    if (escrito) {
                        Text(
                            stringResource(if (document.tipo == "vin") R.string.publish_label_vin else R.string.publish_doc_plate),
                            fontFamily = DmSans,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AdminColors.Muted
                        )
                        SelectionContainer {
                            Text(document.valor, fontFamily = FontFamily.Monospace, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = KarsyInk)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (escrito) AdminActionButton(stringResource(R.string.admin2_curp_copy)) {
                            // La placa o el NIV pueden llevar al dueño: marcado como sensible para que Android
                            // no lo muestre en la vista previa del portapapeles (igual que la CURP).
                            val clip = ClipData.newPlainText(document.tipo, document.valor).apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
                                }
                            }
                            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                            // Desde Android 13 el sistema ya avisa que se copió.
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, copiado, Toast.LENGTH_SHORT).show()
                        } else if (document.tipo in PublishViewModel.TIPOS_DOCUMENTO_FOTO) {
                            AdminActionButton(
                                stringResource(if (document.tipo == "baja") R.string.admin2_repuve_view_baja else R.string.admin2_repuve_view_card)
                            ) { onOpenFile(document.valor) }
                        }
                        AdminActionButton(stringResource(R.string.admin2_repuve_open), tone = ActionTone.Success) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, REPUVE.toUri()))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(context, REPUVE, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
                Text(stringResource(R.string.admin2_repuve_listing_data), fontFamily = DmSans, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AdminColors.Muted)
                AdminField(stringResource(R.string.admin2_repuve_vehicle), vehicle.name)
                AdminField(stringResource(R.string.admin2_repuve_year), vehicle.year.toString())
                if (loaded && document == null) {
                    CasillaRevision(revisada, stringResource(R.string.admin2_repuve_check_reviewed)) { revisada = it }
                } else if (loaded) {
                    CasillaRevision(coincide, stringResource(R.string.admin2_repuve_check_match)) { coincide = it }
                    CasillaRevision(sinRobo, stringResource(R.string.admin2_repuve_check_no_theft)) { sinRobo = it }
                }
                error?.let { Text(it, fontFamily = DmSans, fontSize = 12.sp, color = BadgeTone.Danger.fg) }
            }
        },
        confirmButton = {
            TextButton(onClick = onApprove, enabled = listo) {
                Text(
                    stringResource(R.string.admin2_approve),
                    fontFamily = DmSans,
                    fontWeight = FontWeight.Bold,
                    color = if (listo) BadgeTone.Success.fg else AdminColors.Muted
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onReject, enabled = !working) {
                Text(stringResource(R.string.admin1_reject), fontFamily = DmSans, fontWeight = FontWeight.Bold, color = BadgeTone.Danger.fg)
            }
        }
    )
}
