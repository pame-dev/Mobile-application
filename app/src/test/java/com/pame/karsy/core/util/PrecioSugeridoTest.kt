package com.pame.karsy.core.util

import com.pame.karsy.core.util.PrecioSugerido.Comparacion
import com.pame.karsy.core.util.PrecioSugerido.Rango
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PrecioSugeridoTest {
    private fun miles(vararg n: Int) = n.map { it * 1000.0 }

    @Test fun sinPrecios_noSugiere() = assertNull(PrecioSugerido.calcular(emptyList()))

    @Test fun unSoloPrecio_noSugiere() = assertNull(PrecioSugerido.calcular(miles(200)))

    @Test fun dosPrecios_usaMinYMax() {
        assertEquals(Rango(2, 200_000, 300_000, 250_000), PrecioSugerido.calcular(miles(300, 200)))
    }

    @Test fun cuatroPrecios_usaMinYMaxNoPercentiles() {
        assertEquals(Rango(4, 100_000, 400_000, 250_000), PrecioSugerido.calcular(miles(100, 200, 300, 400)))
    }

    @Test fun cincoPrecios_usaPercentiles20y80() {
        // indices 0.8 y 3.2 interpolados: 180 mil y 420 mil
        assertEquals(Rango(5, 180_000, 420_000, 300_000), PrecioSugerido.calcular(miles(500, 100, 300, 200, 400)))
    }

    @Test fun seisPrecios_medianaParPromediaLosDelMedio() {
        assertEquals(Rango(6, 200_000, 500_000, 350_000), PrecioSugerido.calcular(miles(100, 200, 300, 400, 500, 600)))
    }

    @Test fun tresPrecios_medianaImparEsElDelMedio_yMinMaxCompletos() {
        assertEquals(Rango(3, 100_000, 900_000, 200_000), PrecioSugerido.calcular(miles(900, 100, 200)))
    }

    @Test fun cincoPrecios_unPrecioRaroNoEstiraElRango() {
        val r = PrecioSugerido.calcular(miles(200, 210, 220, 230, 5000))!!
        assertEquals(220_000, r.tipico)
        assertEquals(true, r.max < 5_000_000)
    }

    @Test fun redondeaAMiles() {
        val r = PrecioSugerido.calcular(listOf(100_400.0, 200_600.0))!!
        assertEquals(100_000, r.min)
        assertEquals(201_000, r.max)
        assertEquals(151_000, r.tipico)
    }

    @Test fun ignoraCeroNegativosYNoNumeros() {
        val r = PrecioSugerido.calcular(
            listOf(0.0, -5.0, Double.NaN, Double.POSITIVE_INFINITY, 100_000.0, 200_000.0),
        )
        assertNotNull(r)
        assertEquals(Rango(2, 100_000, 200_000, 150_000), r)
    }

    @Test fun soloPreciosInvalidos_noSugiere() {
        assertNull(PrecioSugerido.calcular(listOf(0.0, -1.0, Double.NaN)))
    }

    @Test fun invalidosNoCuentanParaElMinimo() {
        assertNull(PrecioSugerido.calcular(listOf(0.0, 100_000.0)))
    }

    private val rango = Rango(5, 200_000, 300_000, 250_000)

    @Test fun comparar_igualAlMinimoEsDentro() =
        assertEquals(Comparacion.DENTRO, PrecioSugerido.comparar(200_000, rango))

    @Test fun comparar_igualAlMaximoEsDentro() =
        assertEquals(Comparacion.DENTRO, PrecioSugerido.comparar(300_000, rango))

    @Test fun comparar_unPesoAbajoDelMinimoEsAbajo() =
        assertEquals(Comparacion.ABAJO, PrecioSugerido.comparar(199_999, rango))

    @Test fun comparar_unPesoArribaDelMaximoEsArriba() =
        assertEquals(Comparacion.ARRIBA, PrecioSugerido.comparar(300_001, rango))

    @Test fun comparar_enMedioEsDentro() =
        assertEquals(Comparacion.DENTRO, PrecioSugerido.comparar(250_000, rango))
}
