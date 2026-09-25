package com.pame.karsy.core.util

/**
 * Error con un mensaje escrito para el usuario: mensajeUsuario() lo muestra tal cual.
 * Cualquier otra excepción se muestra como un mensaje genérico.
 */
open class UserFacingException(message: String) : IllegalStateException(message)
