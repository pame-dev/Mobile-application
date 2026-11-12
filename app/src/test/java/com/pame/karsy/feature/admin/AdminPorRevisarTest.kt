package com.pame.karsy.feature.admin

import com.pame.karsy.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class AdminPorRevisarTest {
    @Test fun porRevisarTieneEtiqueta() {
        assertEquals(R.string.admin2_value_to_review, adminValueLabelRes(POR_REVISAR))
    }

    @Test fun todosSigueTeniendoEtiqueta() {
        assertNotNull(adminValueLabelRes("Todos"))
    }
}
