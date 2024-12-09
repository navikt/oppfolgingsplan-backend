package no.nav.syfo.oppfolgingsplan.controller.external

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.syfo.auth.tokenx.TokenXUtil
import no.nav.syfo.auth.tokenx.TokenXUtil.TokenXIssuer.TOKENX
import no.nav.syfo.auth.tokenx.TokenXUtil.fnrFromIdportenTokenX
import no.nav.syfo.metric.Metrikk
import no.nav.syfo.oppfolgingsplan.service.EsyfovarselService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.inject.Inject

@RestController
@ProtectedWithClaims(issuer = TOKENX, claimMap = ["acr=Level4"])
@RequestMapping(value = ["/api/v1/varsel"])
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
