package com.pame.karsy.core.util

import kotlin.coroutines.cancellation.CancellationException

/** Como runCatching, pero deja pasar la cancelación de corrutinas. */
suspend fun <T> safeCall(block: suspend () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
