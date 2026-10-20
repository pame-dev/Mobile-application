package com.pame.karsy.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HuellaVisualTest {
    private val n = HuellaVisual.ANCHO * HuellaVisual.ALTO

    private fun lcg(): DoubleArray {
        var s = 12345L
        return DoubleArray(n) {
            s = (s * 1103515245L + 12345L) % 2147483648L
            ((s shr 16) % 256).toDouble()
        }
    }

    // Los valores esperados salen de web/src/lib/huellaVisual.ts con los mismos arreglos (paridad web/app).
    @Test fun constante_da_ceros() = assertEquals("0000000000000000", HuellaVisual.deGrises(DoubleArray(n) { 100.0 }))

    @Test fun degradeCreciente_da_ceros() =
        assertEquals("0000000000000000", HuellaVisual.deGrises(DoubleArray(n) { ((it % 72) * 3).toDouble() }))

    @Test fun degradeDecreciente_da_unos() =
        assertEquals("ffffffffffffffff", HuellaVisual.deGrises(DoubleArray(n) { (255 - (it % 72) * 3).toDouble() }))

    @Test fun degradeVertical_da_ceros() =
        assertEquals("0000000000000000", HuellaVisual.deGrises(DoubleArray(n) { ((it / 72) * 4).toDouble() }))

    @Test fun tablero_paridadConWeb() = assertEquals(
        "55aa55aa55aa55aa",
        HuellaVisual.deGrises(DoubleArray(n) { (((it % 72) / 8 + (it / 72) / 8) % 2 * 255).toDouble() }),
    )

    @Test fun pseudoaleatorio_paridadConWeb() = assertEquals("aef4a6aa5255be25", HuellaVisual.deGrises(lcg()))

    @Test fun decimales_paridadConWeb() =
        assertEquals("835c8a55835c8a55", HuellaVisual.deGrises(DoubleArray(n) { ((it * 37) % 256) / 3.0 }))

    @Test fun huellaSiempreTiene16Hex() {
        val h = HuellaVisual.deGrises(lcg())
        assertEquals(16, h.length)
        assertTrue(h.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test fun distancia_bordes() {
        assertEquals(0, HuellaVisual.distancia("aef4a6aa5255be25", "aef4a6aa5255be25"))
        assertEquals(64, HuellaVisual.distancia("0000000000000000", "ffffffffffffffff"))
        assertEquals(1, HuellaVisual.distancia("0f0f0f0f0f0f0f0f", "0f0f0f0f0f0f0f0e"))
        assertEquals(HuellaVisual.distancia("0123456789abcdef", "fedcba9876543210"),
            HuellaVisual.distancia("fedcba9876543210", "0123456789abcdef"))
    }

    @Test fun mismaFoto_limiteEnCuatro() {
        val base = "0000000000000000"
        assertTrue(HuellaVisual.mismaFoto(base, base))
        assertTrue(HuellaVisual.mismaFoto(base, "000000000000000f")) // 4 bits
        assertFalse(HuellaVisual.mismaFoto(base, "000000000000001f")) // 5 bits
        assertFalse(HuellaVisual.mismaFoto(base, "ffffffffffffffff"))
    }

    @Test fun fotoRetocada_unPocoDeCambioSigueSiendoLaMisma() {
        val a = lcg()
        val b = a.copyOf()
        // recuadro tipo placa: un bloque de 8x8 con gris uniforme
        for (y in 0 until 8) for (x in 0 until 8) b[(24 + y) * 72 + 16 + x] = 128.0
        val d = HuellaVisual.distancia(HuellaVisual.deGrises(a), HuellaVisual.deGrises(b))
        assertTrue("distancia $d", d <= 8)
    }
}
