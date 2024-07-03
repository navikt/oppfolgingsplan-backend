package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Utenlandsopphold(
    var landkode: String,
    var periode: Periode,
    var rapporteringsperiode: String,
    var sporingsinformasjon: Sporingsinformasjon
) : Serializable
