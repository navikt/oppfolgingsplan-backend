package no.nav.syfo.domain

data class Fodselsnummer(val value: String) {
    private val elevenDigits = Regex("\\d{11}")

    init {
        require(elevenDigits.matches(value)) { "Fodselsnummer must be 11 digits" }
    }
}
