package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class Sporingsinformasjon(
    var endretAv: String,
    var endretKilde: String,
    var endretKildereferanse: String,
    var endretTidspunkt: String,
    var opprettetAv: String,
    var opprettetKilde: String,
    var opprettetKildereferanse: String,
    var opprettetTidspunkt: String
) : Serializable
