package no.nav.syfo.kontaktinfo

data class PostPersonerResponse(
    val personer: Map<String, DigitalKontaktinfo> = mapOf(),
    val feil: Map<String, String> = mapOf(),
)
