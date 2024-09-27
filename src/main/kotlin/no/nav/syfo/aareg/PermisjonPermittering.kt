package no.nav.syfo.aareg

import java.io.Serializable

data class PermisjonPermittering(
    var periode: Periode,
    var permisjonPermitteringId: String,
    var prosent: Double,
    var sporingsinformasjon: Sporingsinformasjon,
    var type: String
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
