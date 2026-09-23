package com.pame.karsy.feature.publish

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Estado del flujo "Publicar vehículo" (4 pasos + overlay de éxito).
 * Los datos del formulario viven aquí para no perderse al ir y volver entre pasos.
 */
class PublishViewModel : ViewModel() {

    var step by mutableIntStateOf(1)
        private set
    var published by mutableStateOf(false)
        private set

    // Paso 1: Datos
    var marca by mutableStateOf("")
    var modelo by mutableStateOf("")
    var anio by mutableStateOf("")
    var precio by mutableStateOf("")
    var transmision by mutableStateOf("")
    var kilometraje by mutableStateOf("")
    var cilindros by mutableStateOf("")
    var caballos by mutableStateOf("")
    var tipo by mutableStateOf("")
    var color by mutableStateOf("")
    var duenos by mutableStateOf("")

    // Paso 3: Detalles
    var descripcion by mutableStateOf("")
    var imperfecciones by mutableStateOf("")

    fun next() {
        if (step < 4) step++
    }

    /** Regresa un paso. Devuelve false si ya estamos en el paso 1 (hay que salir del flujo). */
    fun back(): Boolean {
        if (step == 1) return false
        step--
        return true
    }

    fun publish() {
        // TODO: crear publicaciones + propuestas_publicacion (estado 'pendiente') y subir fotos a Storage -> fotos_propuesta_publicacion
        published = true
    }
}
