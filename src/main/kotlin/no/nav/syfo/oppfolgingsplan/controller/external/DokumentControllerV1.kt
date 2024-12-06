package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.service.PdfService
import no.nav.syfo.tokenx.TokenXUtil
import no.nav.syfo.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.tokenx.TokenXUtil.fnrFromIdportenTokenX
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_PDF
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v2/dokument/{oppfolgingsplanId}/ekstern"])
class DokumentControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val pdfService: PdfService,
    private val metrikk: Metrikk,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    @GetMapping
    fun hentPdf(@PathVariable("oppfolgingsplanId") oppfolgingsplanId: Long): ResponseEntity<*> {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val pdf = pdfService.hentPdf(oppfolgingsplanId, innloggetIdent)
        metrikk.tellHendelse("hent_pdf")
        return ResponseEntity.ok()
            .contentType(APPLICATION_PDF)
            .body(pdf)
    }
}
