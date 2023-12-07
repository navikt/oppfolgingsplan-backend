package no.nav.syfo.kontaktinfo

data class Kontaktinfo(
    val fnr: String,
    val epost: String? = null,
    val tlf: String? = null,
    val skalHaVarsel: Boolean,
)
