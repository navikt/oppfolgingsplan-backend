package no.nav.syfo.person

data class Person(
    val fnr: String,
    val navn: String,
    val pilotUser: Boolean = false,
)
