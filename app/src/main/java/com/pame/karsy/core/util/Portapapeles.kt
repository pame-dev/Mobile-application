package com.pame.karsy.core.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast

/**
 * Copia [texto] al portapapeles. Con [sensible] (p. ej. la CLABE, igual que la CURP) Android no lo
 * muestra en la vista previa del portapapeles. Desde Android 13 el sistema ya avisa que se copió;
 * antes se muestra [avisoCopiado].
 */
fun copiarAlPortapapeles(context: Context, etiqueta: String, texto: String, avisoCopiado: String, sensible: Boolean = false) {
    val clip = ClipData.newPlainText(etiqueta, texto).apply {
        if (sensible && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true) }
        }
    }
    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, avisoCopiado, Toast.LENGTH_SHORT).show()
}
