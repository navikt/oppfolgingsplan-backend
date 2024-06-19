package no.nav.syfo.virksomhet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.ereg.EregConsumer
import no.nav.syfo.domain.Virksomhet
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.util.virksomhetsnummerInvalid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject


@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/virksomhet/{virksomhetsnummer}"])
class VirksomhetController @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val eregConsumer: EregConsumer,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getVirksomhet(
        @PathVariable("virksomhetsnummer") virksomhetsnummer: String,
    ): ResponseEntity<Virksomhet> {
        TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)

        if (virksomhetsnummerInvalid(virksomhetsnummer)) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build()
        }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(
                Virksomhet(
                    virksomhetsnummer = virksomhetsnummer,
                    navn = eregConsumer.virksomhetsnavn(virksomhetsnummer),
                ),
            )
    }
}
