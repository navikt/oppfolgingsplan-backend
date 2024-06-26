package no.nav.syfo.virksomhet

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.ereg.EregClient
import no.nav.syfo.domain.Virksomhet
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.domain.Virksomhetsnummer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/virksomhet/{virksomhetsnummer}"])
class VirksomhetController(
    private val contextHolder: TokenValidationContextHolder,
    private val eregClient: EregClient,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getVirksomhet(
        @PathVariable("virksomhetsnummer") virksomhetsnummer: String,
    ): ResponseEntity<Virksomhet> {
        TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)

        return try {
            val virksomhetsnummerAsObject = Virksomhetsnummer(virksomhetsnummer)
            ResponseEntity
                .status(HttpStatus.OK)
                .body(
                    Virksomhet(
                        virksomhetsnummer = virksomhetsnummerAsObject.value,
                        navn = eregClient.virksomhetsnavn(virksomhetsnummer),
                    ),
                )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()
        }
    }
}
