package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.Unprotected
import no.nav.syfo.pdl.PdlConsumer
import no.nav.syfo.repository.dao.OppfolgingsplanDAO
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.function.Consumer
import javax.inject.Inject

@RestController
@Unprotected
@RequestMapping(value = ["/internal/v2/oppfolgingsplan"])
class NullstillOppfolgingsplanControllerV1 @Inject constructor(
    private val oppfolgingsplanDAO: OppfolgingsplanDAO,
    private val pdlConsumer: PdlConsumer
) {
    @DeleteMapping(path = ["/slett/{id}"])
    fun deleteOppfolgingsplanById(
        @PathVariable("id") id: Long,
        @Value("\${nais.cluster.name}") env: String
    ): ResponseEntity<*> {
        return if (isDev(env)) {
            logger.info("Sletter oppfolgingsplan for id")
            oppfolgingsplanDAO.deleteOppfolgingsplan(id)
            ResponseEntity.ok().build<Any>()
        } else {
            handleEndpointNotAvailableForProdError()
        }
    }

    @DeleteMapping(path = ["/slett/person/{fnr}"])
    fun deleteOppfolgingsplanByFnr(
        @PathVariable("fnr") fnr: String,
        @Value("\${nais.cluster.name}") env: String
    ): ResponseEntity<*> {
        return if (isDev(env)) {
            val aktorId = pdlConsumer.aktorid(fnr)
            val dialogIder = oppfolgingsplanDAO.hentDialogIDerByAktoerId(aktorId)
            logger.info("Sletter oppfolgingsplaner for aktorId")
            dialogIder.forEach(Consumer { oppfolgingsdialogId: Long ->
                oppfolgingsplanDAO.deleteOppfolgingsplan(
                    oppfolgingsdialogId
                )
            })
            ResponseEntity.ok().build<Any>()
        } else {
            handleEndpointNotAvailableForProdError()
        }
    }

    private fun handleEndpointNotAvailableForProdError(): ResponseEntity<*> {
        logger.error("Det ble gjort kall mot 'slett oppfolgingsplan', men dette endepunktet er togglet av og skal aldri brukes i prod.")
        return ResponseEntity.notFound().build<Any>()
    }

    private fun isDev(env: String): Boolean {
        return env == "dev-fss" || env == "local"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NullstillOppfolgingsplanControllerV1::class.java)
    }
}
