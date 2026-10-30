package com.pame.karsy.core.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Aviso de la app con el estilo de Karsy, en lugar del Toast de Android: cualquier pantalla o
 * ViewModel llama [mostrar] y NavGraph lo dibuja abajo (sobre la barra si está visible).
 * Dentro de un AlertDialog no se vería (el diálogo es otra ventana): ahí se sigue usando Toast.
 */
object AvisoKarsy {
    /** Aviso visible: texto, si es un error y un número que cambia en cada llamada (reinicia el tiempo). */
    data class Aviso(val texto: String, val error: Boolean, val id: Int)

    var actual by mutableStateOf<Aviso?>(null)
        private set
    private var contador = 0

    fun mostrar(texto: String, error: Boolean = false) {
        actual = Aviso(texto, error, ++contador)
    }

    fun cerrar() {
        actual = null
    }
}
