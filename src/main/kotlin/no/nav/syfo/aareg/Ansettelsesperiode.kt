package no.nav.syfo.aareg

import java.io.Serializable

data class Ansettelsesperiode(
    var bruksperiode: Bruksperiode? = null,
    var periode: Periode,
    var sporingsinformasjon: Sporingsinformasjon? = null,
    var varslingskode: String? = null
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
