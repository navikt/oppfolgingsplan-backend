package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oppfolgingsplan.domain.KommentarDTO
import no.nav.syfo.oppfolgingsplan.service.KommentarService
import no.nav.syfo.oppfolgingsplan.service.TiltakService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4", "acr=idporten-loa-high"], combineWithOr = true)
@RequestMapping(value = ["/api/v1/tiltak/actions/{id}"])
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
        @RequestBody kommentarDTO: KommentarDTO,
    ): Long {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        val kommentar = kommentarDTO
        val kommentarId = kommentarService.lagreKommentar(id, kommentar, innloggetIdent)
        metrikk.tellHendelse("lagre_kommentar")
        return kommentarId
    }
}
