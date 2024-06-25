package no.nav.syfo.brukertilgang

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BrukertilgangService @Autowired constructor(
    private var brukertilgangClient: BrukertilgangClient
) {

    fun tilgangTilOppslattIdent(innloggetIdent: String, oppslaattFnr: String): Boolean {
        return oppslaattFnr == innloggetIdent || brukertilgangClient.hasAccessToAnsatt(oppslaattFnr)
    }
}

