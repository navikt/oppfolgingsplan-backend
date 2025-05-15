package no.nav.syfo.kontaktinfo

import java.io.Serializable

@Suppress("SerialVersionUIDInSerializableClass")
data class DigitalKontaktinfo(
    val kanVarsles: Boolean = false,
    val reservert: Boolean = true,
    val mobiltelefonnummer: String? = null,
    val epostadresse: String? = null,
) : Serializable

fun DigitalKontaktinfo.toKontaktinfo(fnr: String): Kontaktinfo {
    return Kontaktinfo(
        fnr = fnr,
        epost = this.epostadresse,
        tlf = this.mobiltelefonnummer,
        skalHaVarsel = !this.reservert && this.kanVarsles,
    )
}
