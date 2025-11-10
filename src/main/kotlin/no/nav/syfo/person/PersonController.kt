package no.nav.syfo.person

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.brukertilgang.BrukertilgangService
import no.nav.syfo.pdl.PdlClient
import no.nav.syfo.pdl.fullName
import no.nav.syfo.pdl.isPilotUser
import no.nav.syfo.util.NAV_PERSONIDENT_HEADER
import no.nav.syfo.util.fodselsnummerInvalid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/person"])
class PersonController @Autowired constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val pdlClient: PdlClient,
    private val brukertilgangService: BrukertilgangService,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    @ResponseBody
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPerson(
        @RequestHeader(NAV_PERSONIDENT_HEADER) fnr: String,
    ): ResponseEntity<Person> {
        val innloggetFnr = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        return if (fodselsnummerInvalid(fnr)) {
            LOG.error("Ugyldig fnr ved henting av person")
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .build()
        } else {
            if (!brukertilgangService.tilgangTilOppslattIdent(innloggetFnr, fnr)) {
                LOG.error("Ikke tilgang til person: Bruker spør om noen andre enn seg selv eller egne ansatte")
                ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .build()
            } else {
                val pdlPerson = pdlClient.person(fnr)

                if (pdlPerson?.hentPerson == null) {
                    LOG.error("Person ikke funnet i PDL")
                    return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .build()
                }

               ResponseEntity
                    .status(HttpStatus.OK)
                    .body(
                        Person(
                            fnr = fnr,
                            navn = pdlPerson.fullName() ?: "Ukjent navn",
                            pilotUser = pdlPerson.isPilotUser()
                        ),
                    )
            }
        }
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(PersonController::class.java)
    }
}
