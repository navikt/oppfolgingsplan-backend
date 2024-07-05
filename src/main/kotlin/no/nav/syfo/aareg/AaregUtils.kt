package no.nav.syfo.aareg

import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP

object AaregUtils {
    fun stillingsprosentWithMaxScale(percent: Double): BigDecimal {
        val percentAsBigDecimal = BigDecimal.valueOf(percent)

        return if (percentAsBigDecimal.scale() > 1) {
            percentAsBigDecimal.setScale(1, HALF_UP)
        } else {
            percentAsBigDecimal
        }
    }
}