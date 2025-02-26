package no.nav.syfo.brukertilgang

import no.nav.syfo.cache.ValkeyStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BrukertilgangService @Autowired constructor(
    private var brukertilgangClient: BrukertilgangClient,
    private var valkeyStore: ValkeyStore
) {
    fun tilgangTilOppslattIdent(innloggetIdent: String, oppslaattFnr: String): Boolean {
        val cacheKey = "$innloggetIdent$oppslaattFnr"
        val cachedValue: Boolean? = valkeyStore.getObject(cacheKey, Boolean::class.java)

        if (cachedValue != null) {
            log.info("Using cached value for tilgang")
            return cachedValue
        }

        val hasAccess = oppslaattFnr == innloggetIdent || brukertilgangClient.hasAccessToAnsatt(oppslaattFnr)
        valkeyStore.setObject(cacheKey, hasAccess, 3600)

        return hasAccess
    }

    companion object {
        private val log = LoggerFactory.getLogger(BrukertilgangService::class.java)
    }
}
