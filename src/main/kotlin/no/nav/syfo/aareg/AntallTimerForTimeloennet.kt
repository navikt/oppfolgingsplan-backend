package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class AntallTimerForTimeloennet(
    var antallTimer: Double,
    var periode: Periode,
    var rapporteringsperiode: String,
    var sporingsinformasjon: Sporingsinformasjon
) : Serializable
