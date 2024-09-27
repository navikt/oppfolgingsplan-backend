package no.nav.syfo.aareg

import java.io.Serializable

data class AntallTimerForTimeloennet(
    var antallTimer: Double,
    var periode: Periode,
    var rapporteringsperiode: String,
    var sporingsinformasjon: Sporingsinformasjon
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
