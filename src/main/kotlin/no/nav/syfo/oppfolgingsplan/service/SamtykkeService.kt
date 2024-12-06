package no.nav.syfo.oppfolgingsplan.service

import jakarta.ws.rs.ForbiddenException
import org.springframework.stereotype.Service
import javax.inject.Inject

@Service
class SamtykkeService(
    pdlConsumer: PdlConsumer,
    metrikk: Metrikk,
    oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val tilgangskontrollService: TilgangskontrollService
) {
    private val pdlConsumer: PdlConsumer = pdlConsumer
    private val metrikk: Metrikk = metrikk
    private val oppfolgingsplanDAO: OppfolgingsplanDAO = oppfolgingsplanDAO

    fun giSamtykke(oppfoelgingsdialogId: Long?, fnr: String, giSamtykke: Boolean) {
        val aktoerId: String = pdlConsumer.aktorid(fnr)
        val oppfolgingsplan: Oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfoelgingsdialogId)

        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(fnr, oppfolgingsplan)) {
            throw ForbiddenException("Ikke tilgang")
        }

        metrikk.tellOPSamtykke(giSamtykke)
        if (erArbeidstakeren(oppfolgingsplan, aktoerId)) {
            oppfolgingsplanDAO.lagreSamtykkeSykmeldt(oppfoelgingsdialogId, giSamtykke)
        } else {
            oppfolgingsplanDAO.lagreSamtykkeArbeidsgiver(oppfoelgingsdialogId, giSamtykke)
        }
    }
}
