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
        val cacheKey = "tilgangTilOppslattIdent:$innloggetIdent:$oppslaattFnr"
        val downKey = "tilgangTilOppslattIdent:down:$innloggetIdent:$oppslaattFnr"
        val cachedValue: Boolean? = valkeyStore.getObject(cacheKey, Boolean::class.java)

        if (cachedValue != null) {
            log.debug("Using cached value for tilgang")
            return cachedValue
        }

        val cachedDown: Boolean? = valkeyStore.getObject(downKey, Boolean::class.java)
        if (cachedDown == true) {
            throw DependencyUnavailableException("Syfobrukertilgang unavailable (cached)")
        }

        return try {
            val hasAccess = oppslaattFnr == innloggetIdent || brukertilgangClient.hasAccessToAnsatt(oppslaattFnr)
            valkeyStore.setObject(cacheKey, hasAccess, POSITIVE_CACHE_SECONDS)
            hasAccess
        } catch (e: DependencyUnavailableException) {
            valkeyStore.setObject(downKey, true, NEGATIVE_CACHE_SECONDS)
            throw e
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BrukertilgangService::class.java)
        private const val POSITIVE_CACHE_SECONDS = 3600L
        private const val NEGATIVE_CACHE_SECONDS = 60L
    }
}
