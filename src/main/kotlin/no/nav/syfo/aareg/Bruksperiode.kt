package no.nav.syfo.aareg

import java.io.Serializable

data class Bruksperiode(
    var fom: String? = null,
    var tom: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
