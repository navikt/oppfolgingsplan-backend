package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.model.Naermesteleder
import no.nav.syfo.model.Varseltype
import no.nav.syfo.model.Varseltype.SyfoplangodkjenningNl
import no.nav.syfo.model.Varseltype.SyfoplangodkjenningSyk
import no.nav.syfo.narmesteleder.NarmesteLederConsumer
import no.nav.syfo.oppfolgingsplan.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.repository.dao.OppfolgingsplanDAO
import no.nav.syfo.varsling.EsyfovarselProducer
import no.nav.syfo.varsling.domain.*
import no.nav.syfo.varsling.domain.HendelseType.NL_OPPFOLGINGSPLAN_SENDT_TIL_GODKJENNING
import no.nav.syfo.varsling.domain.HendelseType.SM_OPPFOLGINGSPLAN_SENDT_TIL_GODKJENNING
import org.springframework.stereotype.Service

@Service
class EsyfovarselService(
    private val producer: EsyfovarselProducer,
    private val narmesteLederConsumer: NarmesteLederConsumer,
    private val tilgangskontrollService: TilgangskontrollService,
    private val oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val pdlClient: PdlClient
) {
    fun sendVarselTilNarmesteLeder(
        varseltype: Varseltype,
        narmesteleder: Naermesteleder
    ) {

        val esyfovarselHendelse = NarmesteLederHendelse(
            type = getEsyfovarselHendelseType(varseltype),
            ferdigstill = false,
            data = null,
            narmesteLederFnr = narmesteleder.naermesteLederFnr,
            arbeidstakerFnr = narmesteleder.ansattFnr,
            orgnummer = narmesteleder.orgnummer
        )

        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun sendVarselTilArbeidstaker(
        varseltype: Varseltype,
        arbeidstakerFnr: String,
        orgnummer: String
    ) {
        val esyfovarselHendelse = ArbeidstakerHendelse(
            type = getEsyfovarselHendelseType(varseltype),
            ferdigstill = false,
            data = null,
            arbeidstakerFnr = arbeidstakerFnr,
            orgnummer = orgnummer
        )

        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    fun ferdigstillVarsel(
        innloggetFnr: String,
        oppfolgingsplanId: Long
    ) {
        val oppfolgingsplan = oppfolgingsplanDAO.finnOppfolgingsplanMedId(oppfolgingsplanId)
        if (!tilgangskontrollService.brukerTilhorerOppfolgingsplan(innloggetFnr, oppfolgingsplan)) {
            throw IllegalArgumentException("Bruker forsøker å ferdigstille varsel for plan som ikke tilhørerer vedkommende")
        }
        val aktorId = oppfolgingsplan.arbeidstaker.aktoerId
        val arbeidstakerFnr = oppfolgingsplan.arbeidstaker.fnr ?: pdlConsumer.fnr(aktorId)
        val virksomhetsnummer = oppfolgingsplan.virksomhet.virksomhetsnummer
        val narmesteleder = narmesteLederConsumer.narmesteLeder(arbeidstakerFnr, virksomhetsnummer).get()
        val erSykmeldt = innloggetFnr == arbeidstakerFnr

        if (erSykmeldt) {
            ferdigstillVarselArbeidstaker(
                SyfoplangodkjenningSyk,
                narmesteleder
            )
        } else {
            ferdigstillVarselNarmesteLeder(
                SyfoplangodkjenningNl,
                narmesteleder
            )
        }
    }

    private fun ferdigstillVarselArbeidstaker(
        varseltype: Varseltype,
        narmesteleder: Naermesteleder
    ) {
        val esyfovarselHendelse = ArbeidstakerHendelse(
            type = getEsyfovarselHendelseType(varseltype),
            ferdigstill = true,
            data = null,
            arbeidstakerFnr = narmesteleder.ansattFnr,
            orgnummer = narmesteleder.orgnummer
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    private fun ferdigstillVarselNarmesteLeder(
        varseltype: Varseltype,
        narmesteleder: Naermesteleder
    ) {
        val esyfovarselHendelse = NarmesteLederHendelse(
            type = getEsyfovarselHendelseType(varseltype),
            ferdigstill = true,
            data = null,
            narmesteLederFnr = narmesteleder.naermesteLederFnr,
            arbeidstakerFnr = narmesteleder.ansattFnr,
            orgnummer = narmesteleder.orgnummer
        )
        producer.sendVarselTilEsyfovarsel(esyfovarselHendelse)
    }

    private fun getEsyfovarselHendelseType(varseltype: Varseltype): HendelseType {
        return when (varseltype) {
            SyfoplangodkjenningNl -> NL_OPPFOLGINGSPLAN_SENDT_TIL_GODKJENNING
            SyfoplangodkjenningSyk -> SM_OPPFOLGINGSPLAN_SENDT_TIL_GODKJENNING
        }
    }
}
