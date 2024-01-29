package no.nav.syfo.narmesteleder

import java.time.LocalDate
import java.time.LocalDateTime

data class NarmesteLeder (
    val virksomhetsnummer: String,
    val erAktiv: Boolean,
    val aktivFom: LocalDate,
    val aktivTom: LocalDate?,
    val navn: String? = " ",
    val fnr: String,
    val epost: String?,
    val tlf: String?,
    val sistInnlogget: LocalDateTime?,
    val samtykke: Boolean?
)

fun NarmesteLederRelasjonDTO.mapToNarmesteLeder(): NarmesteLeder {
    return NarmesteLeder(
        virksomhetsnummer = this.virksomhetsnummer,
        navn = this.narmesteLederNavn,
        epost = this.narmesteLederEpost,
        tlf = this.narmesteLederTelefonnummer,
        erAktiv = this.aktivTom == null,
        aktivFom = this.aktivFom,
        aktivTom = this.aktivTom,
        fnr = this.narmesteLederPersonIdentNumber,
        sistInnlogget = null,
        samtykke = null
    )
}