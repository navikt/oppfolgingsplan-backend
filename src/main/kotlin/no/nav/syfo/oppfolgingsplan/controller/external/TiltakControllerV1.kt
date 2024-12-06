package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.api.v2.domain.oppfolgingsplan.KommentarRequest
import no.nav.syfo.api.v2.mapper.toKommentar
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.service.KommentarService
import no.nav.syfo.service.TiltakService
import no.nav.syfo.tokenx.TokenXUtil
import no.nav.syfo.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.tokenx.TokenXUtil.fnrFromIdportenTokenX
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v2/tiltak/actions/{id}"])
class TiltakControllerV1 @Inject constructor(
    private val contextHolder: TokenValidationContextHolder,
    private val kommentarService: KommentarService,
    private val tiltakService: TiltakService,
    private val metrikk: Metrikk,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
) {
    @PostMapping(path = ["/slett"])
    fun slettTiltak(@PathVariable("id") id: Long) {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        tiltakService.slettTiltak(id, innloggetIdent)
        metrikk.tellHendelse("slett_tiltak")
    }

    @PostMapping(path = ["/lagreKommentar"], consumes = [APPLICATION_JSON_VALUE], produces = [APPLICATION_JSON_VALUE])
    fun lagreKommentar(
        @PathVariable("id") id: Long,
        @RequestBody kommentarRequest: KommentarRequest,
    ): Long {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val kommentar = kommentarRequest.toKommentar()
        val kommentarId = kommentarService.lagreKommentar(id, kommentar, innloggetIdent)
        metrikk.tellHendelse("lagre_kommentar")
        return kommentarId
    }
}
