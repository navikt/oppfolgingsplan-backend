package no.nav.syfo.oppfolgingsplan.repository.domain

data class Dokument(
    val pdf: ByteArray,
    val xml: String,
    val uuid: String
)
