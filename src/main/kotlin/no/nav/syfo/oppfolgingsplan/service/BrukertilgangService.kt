package no.nav.syfo.oppfolgingsplan.service

import no.nav.syfo.brukertilgang.BrukertilgangConsumer
import org.springframework.stereotype.Service

@Service
class BrukertilgangService (
    brukertilgangConsumer: BrukertilgangConsumer
) {
    private val brukertilgangConsumer: BrukertilgangConsumer = brukertilgangConsumer

    @Cacheable(
        cacheNames = ["tilgangtilident"],
        key = "#innloggetIdent.concat(#oppslaattFnr)",
        condition = "#innloggetIdent != null && #oppslaattFnr != null"
    )
    fun tilgangTilOppslattIdent(innloggetIdent: String, oppslaattFnr: String): Boolean {
        return oppslaattFnr == innloggetIdent || brukertilgangConsumer.hasAccessToAnsatt(oppslaattFnr)
    }
}
