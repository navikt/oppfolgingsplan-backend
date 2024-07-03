package no.nav.syfo.aareg

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class PermisjonPermittering(
    var periode: Periode,
    var permisjonPermitteringId: String,
    var prosent: Double,
    var sporingsinformasjon: Sporingsinformasjon,
    var type: String
) : Serializable
