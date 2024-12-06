package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.service.EsyfovarselService
import no.nav.syfo.tokenx.TokenXUtil
import no.nav.syfo.tokenx.TokenXUtil.fnrFromIdportenTokenX
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.*
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TokenXUtil.TokenXIssuer.TOKENX, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v2/varsel"])
class VarselControllerV1 @Inject constructor(
    private val metrikk: Metrikk,
    private val contextHolder: TokenValidationContextHolder,
    @Value("\${oppfolgingsplan.frontend.client.id}")
    private val oppfolgingsplanClientId: String,
    private val esyfovarselService: EsyfovarselService,
) {
    @PostMapping(path = ["/{oppfolgingsplanId}/ferdigstill"])
    fun ferdigstillVarsel(
        @PathVariable("oppfolgingsplanId") oppfolgingsplanId: Long,
    ) {
        val innloggetIdent = TokenXUtil.validateTokenXClaims(contextHolder, oppfolgingsplanClientId)
            .fnrFromIdportenTokenX()
            .value
        esyfovarselService.ferdigstillVarsel(innloggetIdent, oppfolgingsplanId)
        metrikk.tellHendelse("call_ferdigstillVarsel")
    }
}
