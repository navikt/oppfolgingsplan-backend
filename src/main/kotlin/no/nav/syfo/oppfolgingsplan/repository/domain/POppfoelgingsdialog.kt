package no.nav.syfo.oppfolgingsplan.repository.domain

import no.nav.syfo.oppfolgingsplan.domain.OppfolgingsplanDTO
import no.nav.syfo.oppfolgingsplan.domain.PersonDTO
import no.nav.syfo.oppfolgingsplan.domain.VirksomhetDTO
import java.time.LocalDateTime

data class POppfoelgingsdialog(
    var id: Long,
    var uuid: String,
    var opprettetAv: String?,
    var aktoerId: String,
    var created: LocalDateTime?,
    var samtykkeArbeidsgiver: Boolean?,
    var virksomhetsnummer: String?,
    var sistEndretAv: String?,
    var samtykkeSykmeldt: Boolean?,
    var sisteInnloggingArbeidsgiver: LocalDateTime?,
    var sisteInnloggingSykmeldt: LocalDateTime?,
    var sistAksessertArbeidsgiver: LocalDateTime?,
    var sistAksessertSykmeldt: LocalDateTime?,
    var sistEndretArbeidsgiver: LocalDateTime?,
    var sistEndretSykmeldt: LocalDateTime?,
    var sistEndret: LocalDateTime?,
    var smFnr: String?,
    var opprettetAvFnr: String?,
    var sistEndretAvFnr: String?
)

fun POppfoelgingsdialog.toOppfolgingsplanDTO(): OppfolgingsplanDTO {
    return OppfolgingsplanDTO(
        id = this.id,
        uuid = this.uuid,
        opprettet = this.created!!,
        sistEndretAvAktoerId = this.sistEndretAv,
        sistEndretAvFnr = this.sistEndretAvFnr,
        opprettetAvAktoerId = this.opprettetAv,
        opprettetAvFnr = this.opprettetAvFnr,
        sistEndretDato = this.sistEndret,
        sistEndretArbeidsgiver = this.sistEndretArbeidsgiver,
        sistEndretSykmeldt = this.sistEndretSykmeldt,
        status = null,
        virksomhet = VirksomhetDTO(
            navn = null,
            virksomhetsnummer = this.virksomhetsnummer!!
        ),
        godkjentPlan = null,
        godkjenninger = emptyList(),
        arbeidsoppgaveListe = emptyList(),
        arbeidsgiver = PersonDTO(
            sistAksessert = this.sistAksessertArbeidsgiver,
            sisteEndring = this.sistEndretArbeidsgiver,
            sistInnlogget = this.sisteInnloggingArbeidsgiver,
            samtykke = this.samtykkeArbeidsgiver,
            aktoerId = null,
            epost = null,
            fnr = null,
            navn = null,
            tlf = null
        ),
        arbeidstaker = PersonDTO(
            aktoerId = this.aktoerId,
            fnr = this.smFnr,
            sistAksessert = this.sistAksessertSykmeldt,
            sisteEndring = this.sistEndretSykmeldt,
            sistInnlogget = this.sisteInnloggingSykmeldt,
            samtykke = this.samtykkeSykmeldt,
            epost = null,
            navn = null,
            tlf = null
        ),
        tiltakListe = emptyList(),
    )
}

fun List<POppfoelgingsdialog>.toOppfolgingsplanDTOList(): List<OppfolgingsplanDTO> {
    return this.map { it.toOppfolgingsplanDTO() }
}
